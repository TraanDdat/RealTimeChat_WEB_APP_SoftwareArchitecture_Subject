import logging
from datetime import datetime, timezone

from bson import ObjectId
from fastapi import HTTPException
from pymongo import ASCENDING
from pymongo.errors import PyMongoError

from zalo_backend.database.l_mongodb import collection, group_messages
from zalo_backend.database.l_postgres import SessionLocal
from zalo_backend.models.mongodb.msg import MessageCreate, message_data
from zalo_backend.schemas.user import UserPublic
from zalo_backend.user_service.addfr_service import get_friends
from zalo_backend.user_service.client import get_user, get_users
from . import s3_storage


def _with_media_urls(message):
    return message.model_copy(update={
        "image": s3_storage.image_url(message.image),
        "attachment": s3_storage.attachment_url(message.attachment),
    })


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
    return [contact for contact in get_users() if contact.id in partner_ids]


def get_messages(user_id: int, partner_id: int):
    messages = collection.find({"$or": [
        {"senderId": user_id, "receiverId": partner_id},
        {"senderId": partner_id, "receiverId": user_id},
    ]}).sort([("createdAt", ASCENDING), ("_id", ASCENDING)])
    return [_with_media_urls(message_data(document)) for document in messages]


def delete_message(message_id: str, user_id: int) -> dict:
    try:
        object_id = ObjectId(message_id)
    except (TypeError, ValueError) as error:
        raise HTTPException(status_code=404, detail="Message not found") from error

    try:
        document = collection.find_one_and_delete({"_id": object_id, "senderId": user_id})
    except PyMongoError as error:
        raise HTTPException(status_code=503, detail="Message storage unavailable") from error
    if document is None:
        raise HTTPException(status_code=404, detail="Message not found")

    for object_key in (document.get("image"), document.get("attachment")):
        if object_key:
            s3_storage.delete_image(object_key)
    return {
        "messageId": str(document["_id"]),
        "senderId": document["senderId"],
        "receiverId": document["receiverId"],
    }


def get_unread_counts(user_id: int) -> dict[str, dict[str, int]]:
    private_counts = collection.aggregate([
        {"$match": {"receiverId": user_id, "readBy": {"$ne": user_id}}},
        {"$group": {"_id": "$senderId", "count": {"$sum": 1}}},
    ])
    users = {str(item["_id"]): item["count"] for item in private_counts}

    from zalo_backend.models.postgres.group_chat import GroupMember

    with SessionLocal() as db:
        group_ids = [
            group_id
            for (group_id,) in db.query(GroupMember.group_id).filter(GroupMember.user_id == user_id).all()
        ]
    groups = {}
    if group_ids:
        group_counts = group_messages.aggregate([
            {"$match": {
                "groupId": {"$in": group_ids},
                "senderId": {"$ne": user_id},
                "readBy": {"$ne": user_id},
            }},
            {"$group": {"_id": "$groupId", "count": {"$sum": 1}}},
        ])
        groups = {str(item["_id"]): item["count"] for item in group_counts}
    return {"users": users, "groups": groups}


def mark_messages_read(user_id: int, sender_id: int) -> None:
    try:
        collection.update_many(
            {
                "receiverId": user_id,
                "senderId": sender_id,
                "readBy": {"$ne": user_id},
            },
            {"$addToSet": {"readBy": user_id}},
        )
    except PyMongoError as error:
        raise HTTPException(status_code=503, detail="Message storage unavailable") from error


def send_message(data: MessageCreate, sender_id: int, receiver_id: int):
    if sender_id == receiver_id:
        raise HTTPException(status_code=400, detail="Cannot send messages to yourself.")
    if get_user(receiver_id) is None:
        raise HTTPException(status_code=404, detail="Receiver not found.")

    image_key = s3_storage.store_image_data_url(data.image) if data.image else None
    attachment_key = None
    attachment_size = None
    if data.attachment:
        attachment_key, attachment_size = s3_storage.store_attachment_data_url(
            data.attachment, data.attachmentName, data.attachmentMimeType,
        )

    now = datetime.now(timezone.utc)
    document = {
        "senderId": sender_id, "receiverId": receiver_id, "text": data.text or None,
        "readBy": [sender_id],
        "image": image_key, "attachment": attachment_key,
        "attachmentName": data.attachmentName, "attachmentMimeType": data.attachmentMimeType,
        "emotion": data.emotion,
        "attachmentSize": attachment_size, "createdAt": now, "updatedAt": now,
    }
    try:
        result = collection.insert_one(document)
    except PyMongoError as error:
        s3_storage.delete_image(image_key)
        s3_storage.delete_image(attachment_key)
        raise HTTPException(status_code=503, detail="Message storage unavailable") from error
    return _with_media_urls(message_data({**document, "_id": result.inserted_id}))