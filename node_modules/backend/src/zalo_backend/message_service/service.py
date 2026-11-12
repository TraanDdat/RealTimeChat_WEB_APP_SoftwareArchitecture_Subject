import logging
from datetime import datetime, timezone

import cloudinary.uploader
from fastapi import HTTPException
from pymongo import ASCENDING

from . import user_client
from zalo_backend.core.config import settings
from zalo_backend.database.l_mongodb import collection
from zalo_backend.models.mongodb.msg import MessageCreate, message_data


logger = logging.getLogger(__name__)


def upload_image(image: str) -> str:
    if not settings.CLOUDINARY_URL:
        raise HTTPException(status_code=503, detail="Image upload is not configured.")
    try:
        return cloudinary.uploader.upload(image, resource_type="image")["secure_url"]
    except Exception as error:
        logger.exception("Image upload failed")
        raise HTTPException(status_code=502, detail="Image upload failed.") from error


def get_contacts(user_id: int):
    return [contact for contact in user_client.get_users() if contact.id != user_id]


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
    return [message_data(document) for document in messages]


def send_message(data: MessageCreate, sender_id: int, receiver_id: int):
    if sender_id == receiver_id:
        raise HTTPException(status_code=400, detail="Cannot send messages to yourself.")
    if user_client.get_user(receiver_id) is None:
        raise HTTPException(status_code=404, detail="Receiver not found.")

    image_url = upload_image(data.image) if data.image else None

    now = datetime.now(timezone.utc)
    document = {
        "senderId": sender_id, "receiverId": receiver_id, "text": data.text or None,
        "image": image_url, "createdAt": now, "updatedAt": now,
    }
    result = collection.insert_one(document)
    return message_data({**document, "_id": result.inserted_id})