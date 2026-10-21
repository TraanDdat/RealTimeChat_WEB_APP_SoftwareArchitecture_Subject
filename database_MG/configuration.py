import os

from pymongo import MongoClient


MONGODB_URI = os.getenv(
	"MONGODB_URI",
	"mongodb+srv://huynhhuuhaitrieu_db_user:50MbNegzYFRm2DDw@cluster0.ubv1k1i.mongodb.net/",
)
MONGODB_DATABASE = os.getenv("MONGODB_DATABASE", "chat_db")
USER_SERVICE_URL = os.getenv("USER_SERVICE_URL", "http://127.0.0.1:8000").rstrip("/")
CLOUDINARY_URL = os.getenv("CLOUDINARY_URL", "")

client = MongoClient(
	MONGODB_URI, serverSelectionTimeoutMS=5000, connect=False, tz_aware=True,
)
db = client[MONGODB_DATABASE]
collection = db["messages"]