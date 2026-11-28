from pymongo import ASCENDING, MongoClient

from zalo_backend.core.config import settings


client = MongoClient(
    settings.MONGODB_URI, serverSelectionTimeoutMS=5000, connect=False, tz_aware=True,
)

db = client[settings.MONGODB_DATABASE]

collection = db["messages"]
group_messages = db["group_messages"]
# user activity tracking: Socket.IO online sessions
online_sessions = db["online_sessions"]


def create_indexes():
    collection.create_index(
        [("senderId", ASCENDING), ("receiverId", ASCENDING), ("createdAt", ASCENDING)],
    )
    collection.create_index([("receiverId", ASCENDING), ("senderId", ASCENDING)])
    collection.create_index([("receiverId", ASCENDING), ("readBy", ASCENDING)])
    group_messages.create_index([("groupId", ASCENDING), ("createdAt", ASCENDING)])
    group_messages.create_index([("senderId", ASCENDING), ("createdAt", ASCENDING)])
    group_messages.create_index([("groupId", ASCENDING), ("readBy", ASCENDING)])
    online_sessions.create_index([("userId", ASCENDING), ("startedAt", ASCENDING)])
    online_sessions.create_index([("sid", ASCENDING)])

def close_mongodb():
    client.close()