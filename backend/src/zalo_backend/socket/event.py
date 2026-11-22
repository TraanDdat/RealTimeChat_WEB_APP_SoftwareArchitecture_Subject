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

# socket id -> user_id
connected_users: dict[str, int] = {}

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