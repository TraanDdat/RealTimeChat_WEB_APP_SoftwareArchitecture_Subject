import logging
from http.cookies import SimpleCookie

import socketio
from fastapi import HTTPException
from fastapi.concurrency import run_in_threadpool

from .dependencies import resolve_user
from zalo_backend.core.config import settings
from zalo_backend.core.security import AUTH_COOKIE, decode_access_token
from zalo_backend.models.mongodb.msg import Message


logger = logging.getLogger(__name__)
sio = socketio.AsyncServer(async_mode="asgi", cors_allowed_origins="*")

# user_id -> socket ids (process-local)
online_users: dict[int, set[str]] = {}


def _user_id_from_cookie(environ) -> int | None:
    morsel = SimpleCookie(environ.get("HTTP_COOKIE", "")).get(AUTH_COOKIE)
    return decode_access_token(morsel.value) if morsel else None

def _user_id_from_bearer(environ) -> int | None:
    authorization = environ.get("HTTP_AUTHORIZATION", "")
    if not authorization.startswith("Bearer "):
        return None

    token = authorization.removeprefix("Bearer ").strip()
    return decode_access_token(token)

def _get_socket_user_id(environ) -> int | None:
    cookie_id = _user_id_from_cookie(environ)
    bearer_id = _user_id_from_bearer(environ)

    if cookie_id is not None and bearer_id is not None:
        if cookie_id != bearer_id: # cannot verify 2 user_id different
            return None
        return cookie_id # return cookie_id from web

    if cookie_id is not None:
        return cookie_id

    if bearer_id is not None:
        return bearer_id
    
    return None

@sio.event
async def connect(sid, environ, auth):
    user_id = _get_socket_user_id(environ)
    if user_id is None:
        return False
    try:
        user = await run_in_threadpool(resolve_user, user_id)
    except HTTPException as e:
        logger.warning("SOCKET USER NOT FOUND: user_id=%s", user_id)
        return False
    await sio.enter_room(sid, str(user.id))
    online_users.setdefault(user.id, set()).add(sid)
    await sio.emit("getOnlineUsers", list(online_users.keys()))


@sio.event
async def disconnect(sid, *args):
    for user_id, sids in list(online_users.items()):
        sids.discard(sid)
        if not sids:
            del online_users[user_id]
    await sio.emit("getOnlineUsers", list(online_users))


async def notify_receiver(receiver_id: int, message: Message):
    try:
        await sio.emit(
            "newMessage", message.model_dump(mode="json", by_alias=True),
            room=str(receiver_id),
        )
    except Exception:
        logger.exception("Realtime delivery failed for message %s", message.id)