import mongomock
import pytest
from fastapi import FastAPI, HTTPException
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from zalo_backend.core.security import get_current_user
from zalo_backend.database.l_postgres import Base
from zalo_backend.message_service import group_service
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
