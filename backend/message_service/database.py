from pymongo import ASCENDING, MongoClient

from .config import MONGODB_DATABASE, MONGODB_URI


client = MongoClient(
    MONGODB_URI, serverSelectionTimeoutMS=5000, connect=False, tz_aware=True,
)
db = client[MONGODB_DATABASE]
collection = db["messages"]


def create_indexes():
    collection.create_index(
        [("senderId", ASCENDING), ("receiverId", ASCENDING), ("createdAt", ASCENDING)],
    )
    collection.create_index([("receiverId", ASCENDING), ("senderId", ASCENDING)])