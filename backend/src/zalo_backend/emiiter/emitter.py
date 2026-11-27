import logging
from zalo_backend.socket.event import sio

logger = logging.getLogger(__name__)

async def emit_to_user(
    receiver_id: int,
    event: str,
    data: dict,
):
    try:
        await sio.emit(
            event,
            data,
            room=str(receiver_id),
        )
    except Exception:
        logger.exception(
            "Realtime delivery failed: event=%s, receiver=%s",
            event,
            receiver_id,
        )