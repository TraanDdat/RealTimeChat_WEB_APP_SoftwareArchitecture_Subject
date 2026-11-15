from datetime import datetime, timedelta, timezone
import jwt
from pwdlib import PasswordHash
from fastapi import Request, Response, HTTPException, Depends, status
from sqlalchemy.orm import Session
from fastapi import Depends
from zalo_backend.core.config import settings
from zalo_backend.database.l_postgres import get_db
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

password_hash = PasswordHash.recommended()
security = HTTPBearer() # use Bearer token for Android authenticate

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

def get_current_user(
    request: Request,
    db: Session = Depends(get_db),
):
    from zalo_backend.user_service.service import get_user_by_id
    auth_header = request.headers.get("Authorization")

    token = None

    if auth_header and auth_header.startswith("Bearer "):
        token = auth_header.split(" ", 1)[1]

    if not token:
        token = request.cookies.get(AUTH_COOKIE)
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated",
        )
    user_id = decode_access_token(token)

    if user_id is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
        )

    user = get_user_by_id(db, user_id)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
        )

    return user