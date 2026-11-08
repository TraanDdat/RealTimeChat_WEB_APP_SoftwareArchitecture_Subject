from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from zalo_backend.core.config import settings
from zalo_backend.database.l_mongodb import create_indexes, close_mongodb
from zalo_backend.database.l_postgres import create_tables, close_postgres, check_postgres


from zalo_backend.routes import login
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

# configure CORS
bkend.add_middleware(
    CORSMiddleware,
    # allow_origins=config.FRONTEND_URLS, #allow specific origins from config
    allow_origins=["*"], #allow all origins for development, can be restricted if needed
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# add router
bkend.include_router(login.login_router, prefix=settings.AUTH_URL)
bkend.include_router(register.register_router, prefix=settings.AUTH_URL)