from unittest.mock import Mock

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from backend.user_service import service
from backend.user_service.database import Base, get_db
from backend.user_service.main import app
from backend.user_service.models import User


USER_DATA = {
    "name": "User 1", "email": "user1@example.com",
    "phone_number": "123", "password": "test-password",
}


@pytest.fixture
def client():
    engine = create_engine(
        "sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool,
    )
    session_factory = sessionmaker(bind=engine)
    Base.metadata.create_all(engine)
    with session_factory() as session:
        session.add(User(id=1, **USER_DATA))
        session.commit()

    def test_db():
        with session_factory() as session:
            yield session

    app.dependency_overrides[get_db] = test_db
    try:
        with TestClient(app) as test_client:
            yield test_client
    finally:
        app.dependency_overrides.pop(get_db, None)
        engine.dispose()


def test_list_users(client):
    response = client.get("/users/")
    assert response.status_code == 200
    assert response.json() == [{"id": 1, **USER_DATA}]


@pytest.mark.parametrize("identifier", ["1", "email/user1@example.com"])
def test_get_user_by_id_or_email(client, identifier):
    response = client.get(f"/users/{identifier}")
    assert response.status_code == 200
    assert response.json() == {"id": 1, **USER_DATA}


@pytest.mark.parametrize("identifier", ["1", "email/user1@example.com"])
def test_update_user_by_id_or_email(client, identifier):
    data = {**USER_DATA, "name": "Updated name"}
    response = client.put(f"/users/{identifier}", json=data)
    assert response.status_code == 200
    assert response.json() == {"id": 1, **data}
    assert client.get("/users/1").json()["name"] == "Updated name"


@pytest.mark.parametrize("identifier", ["1", "email/user1@example.com"])
def test_delete_user_by_id_or_email(client, identifier):
    response = client.delete(f"/users/{identifier}")
    assert response.status_code == 204
    assert response.content == b""
    assert client.get("/users/1").status_code == 404


@pytest.mark.parametrize("identifier", ["99", "email/missing@example.com"])
def test_missing_user(client, identifier):
    assert client.get(f"/users/{identifier}").status_code == 404
    assert client.put(f"/users/{identifier}", json=USER_DATA).status_code == 404
    assert client.delete(f"/users/{identifier}").status_code == 404


def test_create_user_route_delegates_to_service(client, monkeypatch):
    create_user = Mock(return_value={"id": 2, **USER_DATA})
    monkeypatch.setattr(service, "create_user", create_user)
    response = client.post("/users", json=USER_DATA)
    assert response.status_code == 200
    assert response.json() == {"id": 2, **USER_DATA}
    create_user.assert_called_once()
    assert create_user.call_args.args[1].model_dump() == USER_DATA