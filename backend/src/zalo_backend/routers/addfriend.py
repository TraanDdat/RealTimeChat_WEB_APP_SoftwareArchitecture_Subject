from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from zalo_backend.core.security import get_current_user
from zalo_backend.database.l_postgres import get_db
from zalo_backend.schemas.user import AddFriendRequest, AddFriendResponse, FriendRequestResponse, UserPublic
from zalo_backend.user_service import addfr_service
from zalo_backend.user_service.service import get_user
from zalo_backend.socket.eventNotify import notify_rev_friendRequest

addfriend_router = APIRouter()

@addfriend_router.post("/add", status_code=status.HTTP_201_CREATED)
async def addfriend_request(
    request: AddFriendRequest,
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    target_user = get_user(db=db, email=request.target_email)
    if target_user is None:
        raise HTTPException(status_code=404, detail="Target user not found")
    try:
        friendship = addfr_service.create_friend_request(
            db=db, requester_id=current_user.id, addressee_id=target_user.id,
        )
    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error

    notify_rev_friendRequest(
        target_user.id,
        {"id": friendship.id, "sender_id": current_user.id}
    )
    return {
        "data": AddFriendResponse(
            id=friendship.id,
            email=target_user.email,
            username=target_user.username,
            status=friendship.status,
        ),
        "error_code": None,
    }

@addfriend_router.get("/requests")
def get_addfriend_requests(
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    requests = addfr_service.get_pending_friend_requests(db, current_user.id)
    request_list = [
        FriendRequestResponse(
            id=friendship.id,
            email=sender.email,
            username=sender.username,
            status=friendship.status
        )
        for friendship, sender in requests
    ]
    return {"data": request_list, "error_code": None}


def _update_request(friendship_id: int, new_status: str, db: Session, current_user):
    friendship = addfr_service.update_friendship_status(
        db, friendship_id, current_user.id, new_status,
    )
    if friendship is None:
        raise HTTPException(status_code=404, detail="Pending friend request not found")
    return {"data": {"id": friendship.id, "status": friendship.status}, "error_code": None}

def _delete_friendship(
    db: Session,
    current_user_id: int,
    other_user_id: int
):
    friendship = addfr_service.check_friendship(
        db,
        current_user_id,
        other_user_id
    )
    if not friendship or friendship.status != "accepted":
        raise HTTPException(
            status_code=404,
            detail="Friendship not found"
        )
    db.delete(friendship)
    db.commit()

    return {"message": "Friendship deleted successfully"}

@addfriend_router.post("/requests/{friendship_id}/accept")
def accept_friend_request(
    friendship_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    return _update_request(friendship_id, "accepted", db, current_user)


@addfriend_router.post("/requests/{friendship_id}/reject")
def reject_friend_request(
    friendship_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    return _update_request(friendship_id, "rejected", db, current_user)

@addfriend_router.post("/requests/deleted")
def delete_friendship_request(
    other_user_id: int,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    return _delete_friendship(
        db,
        current_user.id,
        other_user_id
    )

@addfriend_router.get("/friends", response_model=list[UserPublic])
def get_friends(
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    return [
        UserPublic.from_user(friend)
        for _, friend in addfr_service.get_friends(db, current_user.id)
    ]