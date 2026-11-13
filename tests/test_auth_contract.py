from types import SimpleNamespace

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from zalo_backend.core.security import hash_password
from zalo_backend.database.l_postgres import get_db
from zalo_backend.message_service.realtime import _user_id_from_cookie
from zalo_backend.routers import auth, login, register
from zalo_backend.user_service import service


@pytest.fixture
def client(monkeypatch):
    user = SimpleNamespace(
        id=7,
        username="Alice",
        email="alice@example.com",
        role="User",
        password=hash_password("test-password"),
        profile_pic=None,
    )
    monkeypatch.setattr(login, "get_user", lambda **kwargs: user)
    monkeypatch.setattr(service, "get_user_by_id", lambda db, user_id: user)
    app = FastAPI()
    app.include_router(auth.auth_router, prefix="/api/auth")
    app.include_router(login.login_router, prefix="/api/auth")
    app.include_router(register.register_router, prefix="/api/auth")
    app.dependency_overrides[get_db] = lambda: object()
    with TestClient(app, base_url="https://testserver") as test_client:
        yield test_client


def test_login_cookie_identifies_same_public_user_in_rest_and_socket(client):
    response = client.post(
        "/api/auth/login",
        json={"email": "alice@example.com", "password": "test-password", "role": "User"},
    )
    assert response.status_code == 200
    assert "httponly" in response.headers["set-cookie"].lower()
    token = client.cookies.get("jwt")
    assert token
    response = client.get("/api/auth/check")
    assert response.status_code == 200
    assert response.json() == {
        "_id": 7,
        "username": "Alice",
        "email": "alice@example.com",
        "profilePic": None,
    }
    assert _user_id_from_cookie({"HTTP_COOKIE": f"jwt={token}"}) == 7


def test_check_without_session_and_bad_login_are_rejected(client):
    assert client.get("/api/auth/check").status_code == 401
    response = client.post(
        "/api/auth/login",
        json={"email": "alice@example.com", "password": "wrong", "role": "User"},
    )
    assert response.status_code == 401
    assert "jwt" not in client.cookies
    assert _user_id_from_cookie({"HTTP_COOKIE": "jwt=invalid"}) is None


def test_create_user_accepts_web_and_android_request_shape(client, monkeypatch):
    created = []
    monkeypatch.setattr(register, "get_user", lambda **kwargs: None)
    monkeypatch.setattr(register, "create_user", lambda db, request: created.append(request.model_dump()))
    payload = {"email": "new@example.com", "username": "New user", "password": "test-password", "role": "User"}
    response = client.post("/api/auth/register", json=payload)
    assert response.status_code == 200
    assert created == [payload]
    assert response.json()["data"] == {"email": "new@example.com", "username": "New user", "role": "User"}