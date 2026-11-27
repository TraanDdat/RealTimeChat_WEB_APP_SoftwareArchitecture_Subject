from fastapi import FastAPI
from fastapi.testclient import TestClient
import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from zalo_backend.core.security import get_current_user
from zalo_backend.database.l_postgres import Base, get_db
from zalo_backend.models.postgres.user import User
from zalo_backend.routers import auth as auth_routes


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
	session.add(User(email="known@example.com", username="Known", password="stored-hash"))
	session.commit()
	try:
		yield session
	finally:
		session.close()
		Base.metadata.drop_all(engine)
		engine.dispose()


def create_auth_app(db_session, current_user=None):
	app = FastAPI()
	app.include_router(auth_routes.auth_router, prefix="/api/auth")
	app.dependency_overrides[get_db] = lambda: db_session
	if current_user is not None:
		app.dependency_overrides[get_current_user] = lambda: current_user
	return app


def test_web_logout_expires_auth_cookie(db_session):
	user = db_session.query(User).filter_by(email="known@example.com").one()
	app = create_auth_app(db_session, user)

	with TestClient(app) as client:
		response = client.post("/api/auth/logout", headers={"X-Client-Type": "web"})

	set_cookie = response.headers.get("set-cookie", "").lower()
	assert response.status_code == 200
	assert "jwt=" in set_cookie
	assert "max-age=0" in set_cookie


def test_login_uses_same_error_for_unknown_email_and_bad_password(db_session, monkeypatch, capsys):
	monkeypatch.setattr(auth_routes, "verify_password", lambda password, hashed: False)
	app = create_auth_app(db_session)

	with TestClient(app) as client:
		known_user = client.post(
			"/api/auth/login",
			json={"email": "known@example.com", "password": "never-log-this"},
		)
		unknown_user = client.post(
			"/api/auth/login",
			json={"email": "unknown@example.com", "password": "never-log-this"},
		)

	assert known_user.status_code == unknown_user.status_code == 401
	assert known_user.json() == unknown_user.json()
	assert "never-log-this" not in capsys.readouterr().out


def test_successful_android_login_does_not_log_password(db_session, monkeypatch, capsys):
	monkeypatch.setattr(auth_routes, "verify_password", lambda password, hashed: True)
	app = create_auth_app(db_session)

	with TestClient(app) as client:
		response = client.post(
			"/api/auth/login",
			headers={"X-Client-Type": "android"},
			json={"email": "known@example.com", "password": "never-log-this"},
		)

	assert response.status_code == 200
	assert response.json()["data"]["access_token"]
	assert "never-log-this" not in capsys.readouterr().out
