from contextlib import asynccontextmanager
import socketio
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException
from zalo_backend.core.config import settings
from zalo_backend.database.l_mongodb import create_indexes, close_mongodb
from zalo_backend.database.l_postgres import create_tables, close_postgres, check_postgres


from zalo_backend.message_service.realtime import sio
from zalo_backend.routes import login
from zalo_backend.routes import messages
from zalo_backend.routes import register

@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        check_postgres()
        create_tables()
        create_indexes()
        yield
    finally:
        close_postgres()
        close_mongodb()

# create a backend server using FastAPI
bkend = FastAPI(title="UI zalo chat demo", lifespan=lifespan)

# configure CORS (credentials require explicit origins, not "*")
bkend.add_middleware(
    CORSMiddleware,
    allow_origins=[settings.FRONTEND_URL],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# frontend reads errors from `response.data.message`
@bkend.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    return JSONResponse(
        {"message": exc.detail, "detail": exc.detail},
        status_code=exc.status_code, headers=getattr(exc, "headers", None),
    )


@bkend.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    error = exc.errors()[0] if exc.errors() else {}
    field = ".".join(str(part) for part in error.get("loc", [])[1:])
    message = f"{field}: {error.get('msg')}" if field else error.get("msg", "Invalid request")
    return JSONResponse({"message": message}, status_code=422)


# add router
bkend.include_router(login.login_router, prefix=settings.AUTH_URL)
bkend.include_router(register.register_router, prefix=settings.AUTH_URL)
bkend.include_router(messages.messages_router, prefix=settings.MESSAGES_URL)

# ASGI entry point: Socket.IO on /socket.io, everything else goes to FastAPI
app = socketio.ASGIApp(sio, other_asgi_app=bkend)