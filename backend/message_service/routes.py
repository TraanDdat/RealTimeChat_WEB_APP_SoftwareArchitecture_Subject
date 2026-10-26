from fastapi import APIRouter, BackgroundTasks, Depends, Path

from . import service
from .dependencies import current_user
from .realtime import notify_receiver
from .schemas import Message, MessageCreate, PublicUser


router = APIRouter(prefix="/api/messages", tags=["messages"])


@router.get("/contacts", response_model=list[PublicUser])
def get_all_contacts(user: PublicUser = Depends(current_user)):
    return service.get_contacts(user.id)


@router.get("/chats", response_model=list[PublicUser])
def get_chat_partners(user: PublicUser = Depends(current_user)):
    return service.get_chat_partners(user.id)


@router.get("/{id}", response_model=list[Message])
def get_messages_by_user_id(
    id: int = Path(gt=0), user: PublicUser = Depends(current_user),
):
    return service.get_messages(user.id, id)


@router.post("/send/{id}", response_model=Message, status_code=201)
def send_message(
    data: MessageCreate, background_tasks: BackgroundTasks,
    id: int = Path(gt=0), user: PublicUser = Depends(current_user),
):
    message = service.send_message(data, user.id, id)
    background_tasks.add_task(notify_receiver, id, message)
    return message