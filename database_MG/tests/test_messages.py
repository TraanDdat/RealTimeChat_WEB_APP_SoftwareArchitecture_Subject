from unittest.mock import AsyncMock

import httpx
import mongomock
import pytest
from fastapi.testclient import TestClient
from pymongo.errors import ConnectionFailure

import main
import user_service


USERS = [
    {"id": user_id, "name": f"User {user_id}", "email": f"user{user_id}@example.com",
     "phone_number": "123", "password": "must-not-be-exposed"}
    for user_id in (1, 2, 3)
]


def headers(user_id=1):
    return {"X-User-ID": str(user_id)}


@pytest.fixture
def client(monkeypatch):
    monkeypatch.setattr(main, "collection", mongomock.MongoClient(tz_aware=True).chat.messages)
    monkeypatch.setattr(main.sio, "emit", AsyncMock())
    main.limiter.storage.reset()

    def user_api(url, **kwargs):
        path = httpx.URL(url).path
        request = httpx.Request("GET", url)
        if path == "/users/":
            return httpx.Response(200, json=USERS, request=request)
        user = next((user for user in USERS if path == f"/users/{user['id']}"), None)
        return httpx.Response(200 if user else 404, json=user, request=request)

    monkeypatch.setattr(user_service.httpx, "get", user_api)
    with TestClient(main.app) as test_client:
        yield test_client


def test_contacts_use_postgres_ids_without_passwords(client):
    response = client.get("/api/messages/contacts", headers=headers())
    assert response.status_code == 200
    assert [user["id"] for user in response.json()] == [2, 3]
    assert all("password" not in user for user in response.json())


def test_send_history_and_private_realtime_event(client):
    response = client.post("/api/messages/send/2", headers=headers(), json={"text": " hello "})
    assert response.status_code == 201
    message = response.json()
    assert message["senderId"] == 1
    assert message["receiverId"] == 2
    assert message["text"] == "hello"
    assert message["createdAt"] == message["updatedAt"]
    assert isinstance(message["_id"], str)
    main.sio.emit.assert_awaited_once_with("newMessage", message, room="2")
    client.post("/api/messages/send/1", headers=headers(2), json={"text": "reply"})
    client.post("/api/messages/send/3", headers=headers(2), json={"text": "private"})
    history = client.get("/api/messages/2", headers=headers()).json()
    assert [item["text"] for item in history] == ["hello", "reply"]
    assert all(item["createdAt"].endswith("Z") for item in history)
    assert client.get("/api/messages/2", headers=headers(3)).json()[0]["text"] == "private"


def test_chat_partners_are_unique_and_bidirectional(client):
    assert client.get("/api/messages/chats", headers=headers()).json() == []
    client.post("/api/messages/send/2", headers=headers(), json={"text": "one"})
    client.post("/api/messages/send/2", headers=headers(), json={"text": "two"})
    client.post("/api/messages/send/1", headers=headers(3), json={"text": "incoming"})
    response = client.get("/api/messages/chats", headers=headers())
    assert response.status_code == 200
    assert {user["id"] for user in response.json()} == {2, 3}
    assert all("password" not in user for user in response.json())


@pytest.mark.parametrize("body", [{}, {"text": "   "}, {"text": "x" * 2001},
                                      {"text": "hello", "senderId": 3}])
def test_invalid_message_body(client, body):
    assert client.post("/api/messages/send/2", headers=headers(), json=body).status_code == 422
    assert main.collection.count_documents({}) == 0


@pytest.mark.parametrize("receiver, status", [(1, 400), (99, 404), (0, 422), ("abc", 422)])
def test_invalid_receiver(client, receiver, status):
    response = client.post(f"/api/messages/send/{receiver}", headers=headers(), json={"text": "hi"})
    assert response.status_code == status
    assert main.collection.count_documents({}) == 0


@pytest.mark.parametrize("user_id, status", [(None, 422), ("abc", 422), (0, 422),
                                            (-1, 422), (99, 404)])
def test_existing_positive_user_id_is_required(client, user_id, status):
    response = client.get(
        "/api/messages/contacts", headers=headers(user_id) if user_id is not None else {},
    )
    assert response.status_code == status


def test_image_upload_and_provider_failure(client, monkeypatch):
    monkeypatch.setattr(main, "CLOUDINARY_URL", "configured")
    calls = []

    def upload(image, **kwargs):
        calls.append(image)
        return {"secure_url": "https://res.cloudinary.com/demo/image/upload/test.png"}

    monkeypatch.setattr(main.cloudinary.uploader, "upload", upload)
    response = client.post("/api/messages/send/2", headers=headers(), json={"image": "data:image/png;base64,aGk="})
    assert response.status_code == 201
    assert response.json()["image"].startswith("https://res.cloudinary.com/")
    assert len(calls) == 1

    def fail_upload(*args, **kwargs):
        raise RuntimeError("provider unavailable")

    monkeypatch.setattr(main.cloudinary.uploader, "upload", fail_upload)
    response = client.post("/api/messages/send/2", headers=headers(), json={"image": "data:image/png;base64,aGk="})
    assert response.status_code == 502
    assert main.collection.count_documents({}) == 1


def test_upstream_and_storage_failures(client, monkeypatch):
    def unavailable(*args, **kwargs):
        raise httpx.ConnectError("offline")

    with monkeypatch.context() as patch:
        patch.setattr(user_service.httpx, "get", unavailable)
        assert client.get("/api/messages/contacts", headers=headers()).status_code == 503

    def storage_unavailable(*args, **kwargs):
        raise ConnectionFailure("offline")

    monkeypatch.setattr(main.collection, "insert_one", storage_unavailable)
    response = client.post("/api/messages/send/2", headers=headers(), json={"text": "hello"})
    assert response.status_code == 503
    main.sio.emit.assert_not_awaited()


def test_socket_connection_checks_user_exists(client, monkeypatch):
    import asyncio

    enter_room = AsyncMock()
    monkeypatch.setattr(main.sio, "enter_room", enter_room)
    for auth in ({}, None, {"user_id": "invalid"}, {"user_id": True},
                 {"user_id": 0}, {"user_id": -1}, {"user_id": 99}):
        assert asyncio.run(main.connect("sid", {}, auth)) is False
    enter_room.assert_not_awaited()
    asyncio.run(main.connect("sid", {}, {"user_id": 2}))
    enter_room.assert_awaited_once_with("sid", "2")


def test_rate_limit_runs_before_user_validation(client):
    for _ in range(120):
        assert client.get("/api/messages/contacts").status_code == 422
    assert client.get("/api/messages/contacts").status_code == 429