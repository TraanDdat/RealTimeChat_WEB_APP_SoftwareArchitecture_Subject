import logging

import socketio
from fastapi import HTTPException
from fastapi.concurrency import run_in_threadpool

from .dependencies import resolve_user
from .schemas import Message


logger = logging.getLogger(__name__)
sio = socketio.AsyncServer(async_mode="asgi")


@sio.event
async def connect(sid, environ, auth):
    user_id = auth.get("user_id") if isinstance(auth, dict) else None
    if type(user_id) is not int or user_id <= 0:
        return False
    try:
        user = await run_in_threadpool(resolve_user, user_id)
    except HTTPException:
        return False
    await sio.enter_room(sid, str(user.id))


async def notify_receiver(receiver_id: int, message: Message):
    try:
        await sio.emit(
            "newMessage", message.model_dump(mode="json", by_alias=True),
            room=str(receiver_id),
        )
    except Exception:
        logger.exception("Realtime delivery failed for message %s", message.id)