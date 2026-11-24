from fastapi import APIRouter, Depends, Path, status
from fastapi.concurrency import run_in_threadpool

from zalo_backend.core.security import get_current_user
from zalo_backend.message_service import service
from zalo_backend.models.mongodb.msg import Message, MessageCreate
from zalo_backend.models.postgres.user import User
from zalo_backend.schemas.user import UserPublic
from zalo_backend.socket.event import sio
from zalo_backend.socket.eventNotify import notify_rev_newMessage

messages_router = APIRouter()


# /contacts and /chats must be declared before /{partner_id}
@messages_router.get("/contacts", response_model=list[UserPublic])
def get_contacts(user: User = Depends(get_current_user)):
    return service.get_contacts(user.id)


@messages_router.get("/chats", response_model=list[UserPublic])
def get_chat_partners(user: User = Depends(get_current_user)):
    return service.get_chat_partners(user.id)


@messages_router.get("/unread-counts")
def get_unread_counts(user: User = Depends(get_current_user)):
    return service.get_unread_counts(user.id)


@messages_router.post("/{partner_id}/read", status_code=status.HTTP_204_NO_CONTENT)
def mark_messages_read(
    partner_id: int = Path(gt=0),
    user: User = Depends(get_current_user),
):
    service.mark_messages_read(user.id, partner_id)


@messages_router.get("/{partner_id}", response_model=list[Message])
def get_messages(partner_id: int = Path(gt=0), user: User = Depends(get_current_user)):
    return service.get_messages(user.id, partner_id)


@messages_router.delete("/{message_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_message(
    message_id: str,
    user: User = Depends(get_current_user),
):
    deleted = await run_in_threadpool(service.delete_message, message_id, user.id)
    payload = {key: value for key, value in deleted.items() if key != "receiverId"}
    await sio.emit("messageDeleted", payload, room=str(deleted["receiverId"]))
    await sio.emit("messageDeleted", payload, room=str(deleted["senderId"]))


@messages_router.post("/send/{receiver_id}", status_code=status.HTTP_201_CREATED, response_model=Message)
async def send_message(
    data: MessageCreate,
    receiver_id: int = Path(gt=0),
    user: User = Depends(get_current_user),
):
    message = await run_in_threadpool(service.send_message, data, user.id, receiver_id)
    await notify_rev_newMessage(receiver_id, message)
    return message
