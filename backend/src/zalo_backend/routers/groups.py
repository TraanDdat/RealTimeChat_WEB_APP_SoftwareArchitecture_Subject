from fastapi import APIRouter, Depends, Path, status
from fastapi.concurrency import run_in_threadpool
from sqlalchemy.orm import Session

from zalo_backend.core.security import get_current_user
from zalo_backend.database.l_postgres import get_db
from zalo_backend.message_service import group_service
from zalo_backend.message_service.realtime import sio
from zalo_backend.models.mongodb.msg import GroupMessage, MessageCreate
from zalo_backend.schemas.groups import (
    GroupCreateRequest,
    GroupMemberAddRequest,
    GroupMemberRoleUpdate,
    GroupResponse,
)


groups_router = APIRouter()


@groups_router.get("", response_model=list[GroupResponse])
def list_groups(db: Session = Depends(get_db), current_user=Depends(get_current_user)):
    return group_service.get_groups(db, current_user.id)


@groups_router.post("", response_model=GroupResponse, status_code=status.HTTP_201_CREATED)
def create_group(
    data: GroupCreateRequest,
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    return group_service.create_group(db, current_user.id, data.name, data.member_ids)


@groups_router.post("/{group_id}/members", response_model=GroupResponse)
def add_member(
    data: GroupMemberAddRequest,
    group_id: int = Path(gt=0),
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    return group_service.add_group_member(
        db, group_id, current_user.id, data.user_id, data.role,
    )


@groups_router.patch("/{group_id}/members/{user_id}/role", response_model=GroupResponse)
def change_member_role(
    data: GroupMemberRoleUpdate,
    group_id: int = Path(gt=0),
    user_id: int = Path(gt=0),
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    return group_service.update_member_role(
        db, group_id, current_user.id, user_id, data.role,
    )


@groups_router.get("/{group_id}/messages", response_model=list[GroupMessage])
def list_group_messages(
    group_id: int = Path(gt=0),
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    return group_service.get_group_messages(db, group_id, current_user.id)


@groups_router.post("/{group_id}/read", status_code=status.HTTP_204_NO_CONTENT)
def mark_group_messages_read(
    group_id: int = Path(gt=0),
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    group_service.mark_group_messages_read(db, group_id, current_user.id)


@groups_router.post(
    "/{group_id}/messages",
    response_model=GroupMessage,
    status_code=status.HTTP_201_CREATED,
)
async def send_group_message(
    data: MessageCreate,
    group_id: int = Path(gt=0),
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    message, member_ids = await run_in_threadpool(
        group_service.send_group_message, db, group_id, current_user.id, data,
    )
    payload = message.model_dump(mode="json", by_alias=True)
    for member_id in member_ids:
        await sio.emit("newGroupMessage", payload, room=str(member_id))
    return message