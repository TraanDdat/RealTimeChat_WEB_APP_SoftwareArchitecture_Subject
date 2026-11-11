from datetime import datetime, timedelta, timezone

import jwt
from fastapi import Response
from pwdlib import PasswordHash

from zalo_backend.core.config import settings

password_hash = PasswordHash.recommended()

AUTH_COOKIE = "jwt"
_JWT_ALGORITHM = "HS256"


def hash_password(password: str) -> str:
    """Hash a plain-text password."""
    return password_hash.hash(password)


def verify_password(
    plain_password: str,
    hashed_password: str,
) -> bool:
    """Verify password against its hash."""
    return password_hash.verify(plain_password, hashed_password)


def create_access_token(user_id: int) -> str:
    expire = datetime.now(timezone.utc) + timedelta(days=settings.JWT_EXPIRE_DAYS)
    return jwt.encode({"sub": str(user_id), "exp": expire}, settings.JWT_SECRET, algorithm=_JWT_ALGORITHM)


def decode_access_token(token: str) -> int | None:
    try:
        payload = jwt.decode(token, settings.JWT_SECRET, algorithms=[_JWT_ALGORITHM])
        return int(payload["sub"])
    except (jwt.InvalidTokenError, KeyError, ValueError):
        return None


def set_auth_cookie(response: Response, user_id: int):
    response.set_cookie(
        AUTH_COOKIE, create_access_token(user_id),
        max_age=settings.JWT_EXPIRE_DAYS * 24 * 3600,
        httponly=True, samesite="strict", secure=settings.COOKIE_SECURE,
    )


def clear_auth_cookie(response: Response):
    response.delete_cookie(AUTH_COOKIE, httponly=True, samesite="strict", secure=settings.COOKIE_SECURE)