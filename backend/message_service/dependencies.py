from fastapi import Header, HTTPException

from . import user_client
from .schemas import PublicUser


def resolve_user(user_id: int) -> PublicUser:
    user = user_client.get_user(user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="User not found.")
    return user


def current_user(user_id: int = Header(alias="X-User-ID", gt=0)) -> PublicUser:
    return resolve_user(user_id)