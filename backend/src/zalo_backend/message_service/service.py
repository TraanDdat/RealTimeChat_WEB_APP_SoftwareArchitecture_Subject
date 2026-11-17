import logging
from datetime import datetime, timezone

from fastapi import HTTPException
from pymongo import ASCENDING
from pymongo.errors import PyMongoError

from . import user_client
from zalo_backend.core.config import settings
from zalo_backend.database.l_mongodb import collection
from zalo_backend.database.l_postgres import SessionLocal
from zalo_backend.models.mongodb.msg import MessageCreate, message_data
from zalo_backend.schemas.user import UserPublic
from zalo_backend.user_service.addfr_service import get_friends
from . import s3_storage


def upload_image(image: str) -> str:
    return s3_storage.store_image_data_url(image)


def _with_image_url(message):
    return message.model_copy(update={"image": s3_storage.image_url(message.image)})


def get_contacts(user_id: int):
    with SessionLocal() as db:
        friendships = get_friends(db, user_id)
        return [UserPublic.from_user(friend) for _, friend in friendships]


def get_chat_partners(user_id: int):
    partner_ids = set(collection.distinct("receiverId", {"senderId": user_id}))
    partner_ids.update(collection.distinct("senderId", {"receiverId": user_id}))
    partner_ids.discard(user_id)
    if not partner_ids:
        return []
    return [contact for contact in user_client.get_users() if contact.id in partner_ids]


def get_messages(user_id: int, partner_id: int):
    messages = collection.find({"$or": [
        {"senderId": user_id, "receiverId": partner_id},
        {"senderId": partner_id, "receiverId": user_id},
    ]}).sort([("createdAt", ASCENDING), ("_id", ASCENDING)])
    return [_with_image_url(message_data(document)) for document in messages]


def send_message(data: MessageCreate, sender_id: int, receiver_id: int):
    if sender_id == receiver_id:
        raise HTTPException(status_code=400, detail="Cannot send messages to yourself.")
    if user_client.get_user(receiver_id) is None:
        raise HTTPException(status_code=404, detail="Receiver not found.")

    image_key = upload_image(data.image) if data.image else None

    now = datetime.now(timezone.utc)
    document = {
        "senderId": sender_id, "receiverId": receiver_id, "text": data.text or None,
        "image": image_key, "createdAt": now, "updatedAt": now,
    }
    try:
        result = collection.insert_one(document)
    except PyMongoError as error:
        s3_storage.delete_image(image_key)
        raise HTTPException(status_code=503, detail="Message storage unavailable") from error
    return _with_image_url(message_data({**document, "_id": result.inserted_id}))