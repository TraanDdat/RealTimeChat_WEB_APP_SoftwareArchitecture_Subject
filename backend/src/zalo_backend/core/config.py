import os
from dotenv import load_dotenv
load_dotenv(".env")

# host and port for the backend server
class Settings:
    # URL
    HOST = os.getenv("HOST", "127.0.0.1")
    PORT = int(os.getenv("PORT", 8000))
    FRONTEND_URL = os.getenv("REACT_APP_API_URL", "http://localhost:5173")
    USER_SERVICE_URL = os.getenv("USER_SERVICE_URL", "http://127.0.0.1:8000").rstrip("/")

    # API
    AUTH_URL = os.getenv("AUTH","/api/auth")
    
    # Postgres
    DATABASE_URL = os.getenv("POSTGRES_DATABASE_URL", "postgresql://pla2hc:bichlan1999@localhost:5432/pla2hc_uizalo_db")
    
    # MongoDB
    MONGODB_DATABASE = os.getenv("MONGODB_DATABASE", "chat_db")
    MONGODB_URI = os.getenv("MONGODB_URI", "mongodb://localhost:27017")

    CLOUDINARY_URL = os.getenv("CLOUDINARY_URL", "")

settings = Settings()