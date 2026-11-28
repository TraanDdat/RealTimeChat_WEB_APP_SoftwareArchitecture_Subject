import os
import secrets
from dotenv import load_dotenv
load_dotenv(".env")

# host and port for the backend server
class Settings:
    ENVIRONMENT = os.getenv("ENVIRONMENT", "development").strip().lower()

    # URL
    HOST = os.getenv("HOST", "127.0.0.1")
    PORT = int(os.getenv("PORT", 8000))
    FRONTEND_URL = os.getenv("REACT_APP_API_URL", "http://127.0.0.1:5173")

    # API
    AUTH_URL = os.getenv("AUTH","/api/auth")
    USER_ADDFRIEND_URL = os.getenv("USER_ADDFRIEND","/api/user/addfriend")
    MESSAGES_URL = os.getenv("MESSAGES_URL","/api/messages")
    GROUP_URL = os.getenv("GROUP_URL","/api/groups")
    STATS_URL = os.getenv("STATS_URL","/api/stats")

    # Keep a stable secret across workers and restarts in production.
    JWT_SECRET = os.getenv("JWT_SECRET")
    if not JWT_SECRET:
        if ENVIRONMENT == "production":
            raise RuntimeError("JWT_SECRET must be configured in production")
        JWT_SECRET = secrets.token_urlsafe(32)
    JWT_EXPIRE_DAYS = int(os.getenv("JWT_EXPIRE_DAYS", 7))
    COOKIE_SECURE = os.getenv(
        "COOKIE_SECURE", "true" if ENVIRONMENT == "production" else "false",
    ).lower() == "true"
    if ENVIRONMENT == "production" and not COOKIE_SECURE:
        raise RuntimeError("COOKIE_SECURE must be true in production")

    CORS_ALLOWED_ORIGINS = os.getenv("CORS_ALLOWED_ORIGINS")
    if not CORS_ALLOWED_ORIGINS:
        if ENVIRONMENT == "production":
            raise RuntimeError("CORS_ALLOWED_ORIGINS must be configured in production")
        CORS_ALLOWED_ORIGINS = "http://localhost:5173,http://127.0.0.1:5173"
    CORS_ALLOWED_ORIGINS = [
        origin.strip().rstrip("/")
        for origin in CORS_ALLOWED_ORIGINS.split(",")
        if origin.strip()
    ]
    
    # Postgres
    DATABASE_URL = os.getenv("POSTGRES_DATABASE_URL", "postgresql://postgres:postgres@database-1.ct4couyucvfp.ap-southeast-2.rds.amazonaws.com:5432/zalo_db")
    
    # MongoDB
    MONGODB_DATABASE = os.getenv("MONGODB_DATABASE", "chat_db")
    MONGODB_URI = os.getenv("MONGODB_URI", "mongodb://127.0.0.1:27017")

    AWS_REGION = os.getenv("AWS_REGION", "ap-southeast-2")
    S3_BUCKET = os.getenv("S3_BUCKET", "s3-zalo")
    S3_PRESIGNED_URL_TTL_SECONDS = int(os.getenv("S3_PRESIGNED_URL_TTL_SECONDS", 3600))

settings = Settings()