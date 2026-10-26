import logging
from contextlib import asynccontextmanager

import socketio
from fastapi import FastAPI
from fastapi.concurrency import run_in_threadpool
from limits import parse
from limits.storage import MemoryStorage
from limits.strategies import MovingWindowRateLimiter
from pymongo.errors import PyMongoError
from starlette.responses import JSONResponse

from . import database
from .realtime import sio
from .routes import router


logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(application):
    try:
        await run_in_threadpool(database.create_indexes)
        yield
    finally:
        database.client.close()


api = FastAPI(title="Chat Message API", lifespan=lifespan)
limiter = MovingWindowRateLimiter(MemoryStorage())
http_rate_limit = parse("120/minute")


@api.middleware("http")
async def rate_limit_requests(request, call_next):
    address = request.client.host if request.client else "unknown"
    if not limiter.hit(http_rate_limit, address):
        return JSONResponse(
            status_code=429, content={"detail": "Too many requests."},
            headers={"Retry-After": "60"},
        )
    return await call_next(request)


@api.exception_handler(PyMongoError)
async def mongo_error_handler(request, error):
    logger.error("MongoDB operation failed", exc_info=error)
    return JSONResponse(status_code=503, content={"detail": "Message storage unavailable."})


api.include_router(router)
app = socketio.ASGIApp(sio, other_asgi_app=api)