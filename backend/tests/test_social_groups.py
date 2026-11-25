from datetime import datetime, timezone

import mongomock
import pytest
from fastapi import FastAPI, HTTPException
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from zalo_backend.core.security import get_current_user
from zalo_backend.database.l_postgres import Base
from zalo_backend.message_service import group_service, s3_storage, service
from zalo_backend.models.mongodb.msg import MessageCreate
from zalo_backend.models.postgres.friendship import Friendship
from zalo_backend.models.postgres.group_chat import GroupMember
from zalo_backend.models.postgres.user import User
from zalo_backend.routers.auth import auth_router
from zalo_backend.user_service.addfr_service import get_friends, update_friendship_status


@pytest.fixture

def db_session():
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    session_factory = sessionmaker(bind=engine)
    session = session_factory()
    session.add_all(
        [
            User(email="one@example.com", username="One", password="hash-one"),
            User(email="two@example.com", username="Two", password="hash-two"),
            User(email="three@example.com", username="Three", password="hash-three"),
        ]
    )
    session.commit()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(engine)
        engine.dispose()


@pytest.mark.parametrize("user_id", [1, 2])
def test_auth_check_returns_frontend_user_identity(db_session, user_id):
    user = db_session.query(User).filter(User.id == user_id).one()
    app = FastAPI()
    app.include_router(auth_router, prefix="/api/auth")
    app.dependency_overrides[get_current_user] = lambda: user

    with TestClient(app) as client:
        response = client.get("/api/auth/check")

    assert response.status_code == 200
    assert response.json() == {
        "_id": user.id,
        "username": user.username,
        "email": user.email,
        "profilePic": user.profile_pic,
    }


def test_contacts_only_include_accepted_friendships(db_session):
    request = Friendship(requester_id=1, addressee_id=2, status="pending")
    db_session.add(request)
    db_session.commit()

    assert get_friends(db_session, 1) == []
    update_friendship_status(db_session, request.id, 2, "accepted")

    friends = get_friends(db_session, 1)
    assert [(friend.id, friend.username) for _, friend in friends] == [(2, "Two")]


def test_group_creation_and_admin_assignment(db_session, monkeypatch):
    collection = mongomock.MongoClient().chat.group_messages
    monkeypatch.setattr(group_service, "group_messages", collection)

    group = group_service.create_group(db_session, 1, "Project team", [2])
    assert [(member.user_id, member.role) for member in group.members] == [
        (1, "admin"),
        (2, "member"),
    ]

    group = group_service.add_group_member(db_session, group.id, 1, 3, "admin")
    assert next(member.role for member in group.members if member.user_id == 3) == "admin"

    message, member_ids = group_service.send_group_message(
        db_session, group.id, 2, MessageCreate(text="hello group"),
    )
    assert message.text == "hello group"
    assert set(member_ids) == {1, 2, 3}
    assert [item.text for item in group_service.get_group_messages(db_session, group.id, 3)] == [
        "hello group",
    ]

    key = "chat-images/group.png"
    monkeypatch.setattr(s3_storage, "store_image_data_url", lambda _: key)
    monkeypatch.setattr(s3_storage, "image_url", lambda image_ref: f"https://signed.test/{image_ref}")
    image_message, _ = group_service.send_group_message(
        db_session, group.id, 2, MessageCreate(image="data:image/png;base64,abc"),
    )
    assert collection.find_one({"image": key})["image"] == key
    assert image_message.image == f"https://signed.test/{key}"

    attachment_key = "chat-attachments/group.pdf"
    monkeypatch.setattr(s3_storage, "store_attachment_data_url", lambda *_: (attachment_key, 24))
    monkeypatch.setattr(
        s3_storage, "attachment_url",
        lambda attachment_ref: f"https://signed.test/{attachment_ref}",
    )
    attachment_message, _ = group_service.send_group_message(
        db_session, group.id, 2,
        MessageCreate(
            attachment="data:application/pdf;base64,JVBERi0xLjQ=",
            attachmentName="group.pdf",
            attachmentMimeType="application/pdf",
        ),
    )
    assert collection.find_one({"attachment": attachment_key})["attachmentSize"] == 24
    assert attachment_message.attachment == f"https://signed.test/{attachment_key}"


def test_only_group_admin_can_add_members(db_session):
    group = group_service.create_group(db_session, 1, "Project team", [2])

    with pytest.raises(HTTPException) as error:
        group_service.add_group_member(db_session, group.id, 2, 3, "member")

    assert error.value.status_code == 403
    assert db_session.query(GroupMember).filter_by(group_id=group.id, user_id=3).first() is None


def test_non_member_cannot_read_group_messages(db_session):
    group = group_service.create_group(db_session, 1, "Private team", [2])

    with pytest.raises(HTTPException) as error:
        group_service.get_group_messages(db_session, group.id, 3)

    assert error.value.status_code == 403


def test_unread_counts_and_mark_read_cover_private_and_group_messages(db_session, monkeypatch):
    private_collection = mongomock.MongoClient().chat.messages
    group_collection = mongomock.MongoClient().chat.group_messages
    group = group_service.create_group(db_session, 1, "Unread team", [2])
    monkeypatch.setattr(service, "collection", private_collection)
    monkeypatch.setattr(service, "group_messages", group_collection)
    monkeypatch.setattr(group_service, "group_messages", group_collection)
    monkeypatch.setattr(service, "SessionLocal", lambda: db_session)
    now = datetime.now(timezone.utc)
    private_collection.insert_many([
        {"senderId": 2, "receiverId": 1, "readBy": [2], "createdAt": now},
        {"senderId": 2, "receiverId": 1, "readBy": [2], "createdAt": now},
        {"senderId": 1, "receiverId": 2, "readBy": [1], "createdAt": now},
    ])
    group_collection.insert_many([
        {"groupId": group.id, "senderId": 2, "readBy": [2], "createdAt": now},
        {"groupId": group.id, "senderId": 1, "readBy": [1], "createdAt": now},
    ])

    assert service.get_unread_counts(1) == {
        "users": {"2": 2},
        "groups": {str(group.id): 1},
    }
    service.mark_messages_read(1, 2)
    group_service.mark_group_messages_read(db_session, group.id, 1)
    assert service.get_unread_counts(1) == {"users": {}, "groups": {}}


def test_private_message_delete_requires_sender_and_cleans_s3(monkeypatch):
    collection = mongomock.MongoClient().chat.messages
    keys_deleted = []
    message_id = collection.insert_one({
        "senderId": 1,
        "receiverId": 2,
        "image": "chat-images/message.png",
        "attachment": "chat-attachments/file.pdf",
    }).inserted_id
    monkeypatch.setattr(service, "collection", collection)
    monkeypatch.setattr(s3_storage, "delete_image", keys_deleted.append)

    with pytest.raises(HTTPException) as error:
        service.delete_message(str(message_id), 2)
    assert error.value.status_code == 404
    assert collection.count_documents({"_id": message_id}) == 1

    result = service.delete_message(str(message_id), 1)

    assert result == {"messageId": str(message_id), "senderId": 1, "receiverId": 2}
    recalled = collection.find_one({"_id": message_id})
    assert recalled["isRecalled"] is True
    assert recalled["image"] is None and recalled["attachment"] is None
    assert keys_deleted == ["chat-images/message.png", "chat-attachments/file.pdf"]

    with pytest.raises(HTTPException) as error:
        service.delete_message(str(message_id), 1)
    assert error.value.status_code == 404


def test_group_message_delete_requires_sender_and_cleans_s3(db_session, monkeypatch):
    collection = mongomock.MongoClient().chat.group_messages
    keys_deleted = []
    group = group_service.create_group(db_session, 1, "Delete test", [2])
    message_id = collection.insert_one({
        "groupId": group.id,
        "senderId": 2,
        "image": "chat-images/group.png",
        "attachment": None,
    }).inserted_id
    monkeypatch.setattr(group_service, "group_messages", collection)
    monkeypatch.setattr(s3_storage, "delete_image", keys_deleted.append)

    with pytest.raises(HTTPException) as error:
        group_service.delete_group_message(db_session, group.id, str(message_id), 1)
    assert error.value.status_code == 404
    assert collection.count_documents({"_id": message_id}) == 1

    result = group_service.delete_group_message(db_session, group.id, str(message_id), 2)

    assert result == {
        "messageId": str(message_id),
        "groupId": group.id,
        "memberIds": [1, 2],
    }
    recalled = collection.find_one({"_id": message_id})
    assert recalled["isRecalled"] is True
    assert recalled["image"] is None
    assert keys_deleted == ["chat-images/group.png"]
