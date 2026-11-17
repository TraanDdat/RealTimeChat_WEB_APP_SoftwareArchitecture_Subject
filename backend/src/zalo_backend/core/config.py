import os
import secrets
from dotenv import load_dotenv
load_dotenv(".env")

# host and port for the backend server
class Settings:
    # URL
    HOST = os.getenv("HOST", "127.0.0.1")
    PORT = int(os.getenv("PORT", 8000))
    FRONTEND_URL = os.getenv("REACT_APP_API_URL", "http://127.0.0.1:5173")

    # API
    AUTH_URL = os.getenv("AUTH","/api/auth")
    USER_URL = os.getenv("USER","/api/user")
    USER_ADDFRIEND_URL = os.getenv("USER_ADDFRIEND","/api/user/addfriend")
    MESSAGES_URL = "/api/messages"

    # Auth: without JWT_SECRET a random one is used, so sessions reset on restart
    JWT_SECRET = os.getenv("JWT_SECRET") or secrets.token_urlsafe(32)
    JWT_EXPIRE_DAYS = int(os.getenv("JWT_EXPIRE_DAYS", 7))
    COOKIE_SECURE = os.getenv("COOKIE_SECURE", "false").lower() == "true"
    
    # Postgres
    DATABASE_URL = os.getenv("POSTGRES_DATABASE_URL", "postgresql://postgres:postgres@database-1.ct4couyucvfp.ap-southeast-2.rds.amazonaws.com:5432/zalo_db")
    
    # MongoDB
    MONGODB_DATABASE = os.getenv("MONGODB_DATABASE", "chat_db")
    MONGODB_URI = os.getenv("MONGODB_URI", "mongodb://127.0.0.1:27017")

    AWS_REGION = os.getenv("AWS_REGION", "ap-southeast-2")
    S3_BUCKET = os.getenv("S3_BUCKET", "s3-zalo")
    S3_PRESIGNED_URL_TTL_SECONDS = int(os.getenv("S3_PRESIGNED_URL_TTL_SECONDS", 3600))

settings = Settings()