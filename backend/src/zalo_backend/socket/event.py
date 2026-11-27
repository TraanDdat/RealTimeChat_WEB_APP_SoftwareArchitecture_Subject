from fastapi.concurrency import run_in_threadpool
from zalo_backend.core.config import settings
from zalo_backend.socket.socketService import get_socket_user_id
from zalo_backend.user_service.client import get_user
import socketio
import logging

logger = logging.getLogger(__name__)
sio = socketio.AsyncServer(
    async_mode="asgi",
    cors_allowed_origins=settings.CORS_ALLOWED_ORIGINS,
)

# user_id -> socket ids (process-local)
online_users: dict[int, set[str]] = {}

# socket id -> user_id
connected_users: dict[str, int] = {}

@sio.event
async def connect(sid, environ, auth):
    user_id = get_socket_user_id(environ)
    if user_id is None:
        return False
    user = await run_in_threadpool(get_user, user_id)
    if user is None:
        logger.warning("SOCKET USER NOT FOUND: user_id=%s", user_id)
        return False
    old_sids = online_users.get(user.id, set()).copy()
    for old_sid in old_sids:
        await sio.emit(
            "forceLogout",
            {"message": "Your account was logged in on another device"},
            to=old_sid
        )
        await sio.disconnect(old_sid)
    await sio.enter_room(sid, str(user.id))
    online_users[user.id] = {sid}
    connected_users[sid] = user.id

    await sio.emit("getOnlineUsers", list(online_users.keys()))
    await sio.enter_room(sid, str(user.id))
    online_users.setdefault(user.id, set()).add(sid)
    connected_users[sid] = user.id
    logger.info("SOCKET CONNECTED: user_id=%s, sid=%s", user.id, sid)
    await sio.emit("getOnlineUsers", list(online_users.keys()))

@sio.event
async def disconnect(sid, *args):
    user_id = connected_users.pop(sid, None)
    if user_id is not None:
        sids = online_users.get(user_id)
        if sids:
            sids.discard(sid)
            if not sids:
                del online_users[user_id]
    await sio.emit("getOnlineUsers", list(online_users))

def _get_user_id_from_sid(sid: str):
    return connected_users.get(sid)

@sio.on("typing")
async def handle_typing(sid, data):
    sender_id = _get_user_id_from_sid(sid)
    if sender_id is None or not isinstance(data, dict):
        return
    receiver_id = data.get("receiver_id")
    if not isinstance(receiver_id, int):
        return
    await sio.emit("userTyping", {"sender_id": sender_id}, room=str(receiver_id))

@sio.on("stopTyping")
async def handle_stop_typing(sid, data):
    sender_id = _get_user_id_from_sid(sid)
    if sender_id is None or not isinstance(data, dict):
        return
    receiver_id = data.get("receiver_id")
    if not isinstance(receiver_id, int):
        return
    await sio.emit("userStopTyping", {"sender_id": sender_id}, room=str(receiver_id))