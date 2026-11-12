from typing import Optional

from fastapi import Cookie, HTTPException

from . import user_client
from zalo_backend.core.security import AUTH_COOKIE, decode_access_token
from zalo_backend.schemas.user import UserPublic


def resolve_user(user_id: int) -> UserPublic:
    user = user_client.get_user(user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="User not found.")
    return user


def current_user(token: Optional[str] = Cookie(default=None, alias=AUTH_COOKIE)) -> UserPublic:
    user_id = decode_access_token(token) if token else None
    user = user_client.get_user(user_id) if user_id else None
    if user is None:
        raise HTTPException(status_code=401, detail="Unauthorized - invalid or missing token.")
    return user