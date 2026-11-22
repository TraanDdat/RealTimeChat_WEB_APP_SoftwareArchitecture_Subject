# event notify from SocketIO
from zalo_backend.models.mongodb.msg import Message
from zalo_backend.emiiter.emitter import emit_to_user

async def notify_rev_newMessage(receiver_id: int, message: Message):
    await emit_to_user(
        receiver_id,
        "newMessage",
        message.model_dump(mode="json", by_alias=True),
    )

async def notify_rev_friendRequest(receiver_id: int, payload: dict):
    await emit_to_user(
        receiver_id,
        "friendRequest",
        payload
    )

async def notify_rev_acceptFriendRequest(receiver_id: int, payload: dict):
    await emit_to_user(
        receiver_id,
        "acceptFriendRequest",
        payload
    )

async def notify_rev_rejectFriendRequest(receiver_id: int, payload: dict):
    await emit_to_user(
        receiver_id,
        "rejectFriendRequest",
        payload
    )