from fastapi import APIRouter, Depends, Path, status
from fastapi.concurrency import run_in_threadpool

from zalo_backend.message_service import service
from zalo_backend.message_service.dependencies import current_user
from zalo_backend.message_service.realtime import notify_receiver
from zalo_backend.models.mongodb.msg import Message, MessageCreate
from zalo_backend.schemas.user import UserPublic

messages_router = APIRouter()


# /contacts and /chats must be declared before /{partner_id}
@messages_router.get("/contacts", response_model=list[UserPublic])
def get_contacts(user: UserPublic = Depends(current_user)):
    return service.get_contacts(user.id)


@messages_router.get("/chats", response_model=list[UserPublic])
def get_chat_partners(user: UserPublic = Depends(current_user)):
    return service.get_chat_partners(user.id)


@messages_router.get("/{partner_id}", response_model=list[Message])
def get_messages(partner_id: int = Path(gt=0), user: UserPublic = Depends(current_user)):
    return service.get_messages(user.id, partner_id)


@messages_router.post("/send/{receiver_id}", status_code=status.HTTP_201_CREATED, response_model=Message)
async def send_message(
    data: MessageCreate,
    receiver_id: int = Path(gt=0),
    user: UserPublic = Depends(current_user),
):
    message = await run_in_threadpool(service.send_message, data, user.id, receiver_id)
    await notify_receiver(receiver_id, message)
    return message
