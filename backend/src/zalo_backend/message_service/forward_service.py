from datetime import datetime, timezone

from bson import ObjectId
from bson.errors import InvalidId
from fastapi import HTTPException
from pymongo.errors import PyMongoError
from sqlalchemy.orm import Session

from zalo_backend.database.l_mongodb import collection, group_messages
from zalo_backend.message_service import s3_storage
from zalo_backend.message_service.group_service import _group_or_404, _membership
from zalo_backend.models.mongodb.msg import ForwardRequest, GroupMessage, Message
from zalo_backend.models.postgres.group_chat import GroupMember
from zalo_backend.models.postgres.user import User

_CONTENT_FIELDS = ("text", "emotion", "attachmentName", "attachmentMimeType", "attachmentSize")


def _load_source(db: Session, user_id: int, request: ForwardRequest) -> dict:
    try:
        object_id = ObjectId(request.messageId)
    except (InvalidId, TypeError) as error:
        raise HTTPException(status_code=404, detail="Message not found") from error

    try:
        if request.sourceGroupId is not None:
            _group_or_404(db, request.sourceGroupId)
            _membership(db, request.sourceGroupId, user_id)
            document = group_messages.find_one({"_id": object_id, "groupId": request.sourceGroupId})
        else:
            document = collection.find_one({
                "_id": object_id,
                "$or": [{"senderId": user_id}, {"receiverId": user_id}],
            })
    except PyMongoError as error:
        raise HTTPException(status_code=503, detail="Message storage unavailable") from error

    if document is None:
        raise HTTPException(status_code=404, detail="Message not found")
    if document.get("isRecalled"):
        raise HTTPException(status_code=400, detail="A recalled message cannot be forwarded")
    return document


def _with_media_urls(message):
    return message.model_copy(update={
        "image": s3_storage.image_url(message.image),
        "attachment": s3_storage.attachment_url(message.attachment),
    })


def forward_message(
    db: Session,
    user_id: int,
    request: ForwardRequest,
) -> tuple[list[Message], list[tuple[GroupMessage, list[int]]]]:
    """Copy a message (text, emoji, image, document or video) into other conversations.

    Each copy gets its own S3 object, so recalling the original never breaks a forwarded copy.
    Returns the new private messages and, for each group, the new message plus its member ids.
    """
    source = _load_source(db, user_id, request)

    receiver_ids = list(dict.fromkeys(request.receiverIds))
    group_ids = list(dict.fromkeys(request.groupIds))
    if user_id in receiver_ids:
        raise HTTPException(status_code=400, detail="Cannot forward a message to yourself")
    if receiver_ids:
        found = {uid for (uid,) in db.query(User.id).filter(User.id.in_(receiver_ids)).all()}
        missing = set(receiver_ids) - found
        if missing:
            raise HTTPException(status_code=404, detail=f"Users not found: {sorted(missing)}")
    group_members: dict[int, list[int]] = {}
    for group_id in group_ids:
        _group_or_404(db, group_id)
        _membership(db, group_id, user_id)
        group_members[group_id] = [
            member_id
            for (member_id,) in db.query(GroupMember.user_id).filter(GroupMember.group_id == group_id).all()
        ]
    sender = db.query(User).filter(User.id == user_id).first()

    created_keys: list[str] = []

    def new_document(**target) -> dict:
        image_key = s3_storage.copy_object(source.get("image"))
        attachment_key = s3_storage.copy_object(source.get("attachment"))
        for original, copied in ((source.get("image"), image_key), (source.get("attachment"), attachment_key)):
            if copied and copied != original:
                created_keys.append(copied)
        now = datetime.now(timezone.utc)
        return {
            **target,
            "senderId": user_id,
            "readBy": [user_id],
            **{field: source.get(field) for field in _CONTENT_FIELDS},
            "image": image_key,
            "attachment": attachment_key,
            "isForwarded": True,
            "createdAt": now,
            "updatedAt": now,
        }

    try:
        private_docs = [new_document(receiverId=receiver_id) for receiver_id in receiver_ids]
        group_docs = [
            new_document(groupId=group_id, senderUsername=sender.username) for group_id in group_ids
        ]
        if private_docs:
            collection.insert_many(private_docs)
        if group_docs:
            group_messages.insert_many(group_docs)
    except (HTTPException, PyMongoError) as error:
        for key in created_keys:
            s3_storage.delete_image(key)
        if isinstance(error, HTTPException):
            raise
        raise HTTPException(status_code=503, detail="Message storage unavailable") from error

    private_messages = [
        _with_media_urls(Message.model_validate({**doc, "_id": str(doc["_id"])})) for doc in private_docs
    ]
    group_results = [
        (
            _with_media_urls(GroupMessage.model_validate({**doc, "_id": str(doc["_id"])})),
            group_members[doc["groupId"]],
        )
        for doc in group_docs
    ]
    return private_messages, group_results
