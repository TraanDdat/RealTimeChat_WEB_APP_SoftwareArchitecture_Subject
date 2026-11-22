from fastapi import HTTPException
from fastapi.concurrency import run_in_threadpool
from zalo_backend.message_service.dependencies import resolve_user
from zalo_backend.socket.socketService import get_socket_user_id
import socketio
import logging

logger = logging.getLogger(__name__)
sio = socketio.AsyncServer(async_mode="asgi", cors_allowed_origins="*")

# user_id -> socket ids (process-local)
online_users: dict[int, set[str]] = {}

@sio.event
async def connect(sid, environ, auth):
    user_id = get_socket_user_id(environ)
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