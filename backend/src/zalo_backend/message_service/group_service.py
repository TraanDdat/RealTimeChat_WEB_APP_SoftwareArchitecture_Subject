from datetime import datetime, timezone

from fastapi import HTTPException
from pymongo.errors import PyMongoError
from sqlalchemy.orm import Session

from zalo_backend.database.l_mongodb import group_messages
from zalo_backend.message_service.service import upload_image
from zalo_backend.message_service import s3_storage
from zalo_backend.models.mongodb.msg import GroupMessage, MessageCreate
from zalo_backend.models.postgres.group_chat import GroupChat, GroupMember
from zalo_backend.models.postgres.user import User
from zalo_backend.schemas.groups import GroupMemberResponse, GroupResponse


def _group_or_404(db: Session, group_id: int) -> GroupChat:
    group = db.query(GroupChat).filter(GroupChat.id == group_id).first()
    if group is None:
        raise HTTPException(status_code=404, detail="Group not found")
    return group


def _membership(db: Session, group_id: int, user_id: int) -> GroupMember:
    membership = db.query(GroupMember).filter_by(group_id=group_id, user_id=user_id).first()
    if membership is None:
        raise HTTPException(status_code=403, detail="You are not a member of this group")
    return membership


def _require_admin(db: Session, group_id: int, user_id: int) -> GroupMember:
    membership = _membership(db, group_id, user_id)
    if membership.role != "admin":
        raise HTTPException(status_code=403, detail="Group admin privileges required")
    return membership


def _group_response(db: Session, group: GroupChat) -> GroupResponse:
    members = (
        db.query(GroupMember, User)
        .join(User, User.id == GroupMember.user_id)
        .filter(GroupMember.group_id == group.id)
        .order_by(GroupMember.joined_at, GroupMember.user_id)
        .all()
    )
    return GroupResponse(
        id=group.id,
        name=group.name,
        created_by=group.created_by,
        created_at=group.created_at,
        members=[
            GroupMemberResponse(
                user_id=member.user_id,
                username=user.username,
                email=user.email,
                role=member.role,
            )
            for member, user in members
        ],
    )


def create_group(db: Session, creator_id: int, name: str, member_ids: list[int]) -> GroupResponse:
    requested_ids = set(member_ids) - {creator_id}
    users = db.query(User.id).filter(User.id.in_(requested_ids)).all() if requested_ids else []
    found_ids = {user_id for (user_id,) in users}
    missing_ids = requested_ids - found_ids
    if missing_ids:
        raise HTTPException(status_code=404, detail=f"Users not found: {sorted(missing_ids)}")

    group = GroupChat(name=name.strip(), created_by=creator_id)
    try:
        db.add(group)
        db.flush()
        db.add(GroupMember(group_id=group.id, user_id=creator_id, role="admin", added_by=creator_id))
        db.add_all(
            GroupMember(group_id=group.id, user_id=user_id, role="member", added_by=creator_id)
            for user_id in requested_ids
        )
        db.commit()
        db.refresh(group)
    except Exception:
        db.rollback()
        raise
    return _group_response(db, group)


def get_groups(db: Session, user_id: int) -> list[GroupResponse]:
    groups = (
        db.query(GroupChat)
        .join(GroupMember, GroupMember.group_id == GroupChat.id)
        .filter(GroupMember.user_id == user_id)
        .order_by(GroupChat.created_at.desc(), GroupChat.id.desc())
        .all()
    )
    return [_group_response(db, group) for group in groups]


def add_group_member(
    db: Session,
    group_id: int,
    actor_id: int,
    user_id: int,
    role: str,
) -> GroupResponse:
    group = _group_or_404(db, group_id)
    _require_admin(db, group_id, actor_id)
    if db.query(User.id).filter(User.id == user_id).first() is None:
        raise HTTPException(status_code=404, detail="User not found")
    if db.query(GroupMember.id).filter_by(group_id=group_id, user_id=user_id).first():
        raise HTTPException(status_code=409, detail="User is already a group member")
    db.add(GroupMember(group_id=group_id, user_id=user_id, role=role, added_by=actor_id))
    db.commit()
    return _group_response(db, group)


def update_member_role(
    db: Session,
    group_id: int,
    actor_id: int,
    user_id: int,
    role: str,
) -> GroupResponse:
    group = _group_or_404(db, group_id)
    _require_admin(db, group_id, actor_id)
    member = _membership(db, group_id, user_id)
    if member.role == "admin" and role != "admin":
        admin_count = db.query(GroupMember).filter_by(group_id=group_id, role="admin").count()
        if admin_count <= 1:
            raise HTTPException(status_code=400, detail="A group must have at least one admin")
    member.role = role
    db.commit()
    return _group_response(db, group)


def get_group_messages(db: Session, group_id: int, user_id: int) -> list[GroupMessage]:
    _group_or_404(db, group_id)
    _membership(db, group_id, user_id)
    try:
        documents = group_messages.find({"groupId": group_id}).sort(
            [("createdAt", 1), ("_id", 1)]
        )
        return [
            message.model_copy(update={
                "image": s3_storage.image_url(message.image),
                "attachment": s3_storage.attachment_url(message.attachment),
            })
            for message in (
                GroupMessage.model_validate({**document, "_id": str(document["_id"])})
                for document in documents
            )
        ]
    except PyMongoError as error:
        raise HTTPException(status_code=503, detail="Group message storage unavailable") from error


def send_group_message(
    db: Session,
    group_id: int,
    sender_id: int,
    data: MessageCreate,
) -> tuple[GroupMessage, list[int]]:
    _group_or_404(db, group_id)
    _membership(db, group_id, sender_id)
    sender = db.query(User).filter(User.id == sender_id).first()
    image_key = upload_image(data.image) if data.image else None
    attachment_key = None
    attachment_size = None
    if data.attachment:
        attachment_key, attachment_size = s3_storage.store_attachment_data_url(
            data.attachment, data.attachmentName, data.attachmentMimeType,
        )
    now = datetime.now(timezone.utc)
    document = {
        "groupId": group_id,
        "senderId": sender_id,
        "senderUsername": sender.username,
        "text": data.text or None,
        "image": image_key,
        "attachment": attachment_key,
        "attachmentName": data.attachmentName,
        "attachmentMimeType": data.attachmentMimeType,
        "attachmentSize": attachment_size,
        "createdAt": now,
        "updatedAt": now,
    }
    try:
        result = group_messages.insert_one(document)
        message = GroupMessage.model_validate({**document, "_id": str(result.inserted_id)})
    except PyMongoError as error:
        s3_storage.delete_image(image_key)
        s3_storage.delete_image(attachment_key)
        raise HTTPException(status_code=503, detail="Group message storage unavailable") from error
    member_ids = [
        user_id
        for (user_id,) in db.query(GroupMember.user_id).filter(GroupMember.group_id == group_id).all()
    ]
    return message.model_copy(update={
        "image": s3_storage.image_url(message.image),
        "attachment": s3_storage.attachment_url(message.attachment),
    }), member_ids