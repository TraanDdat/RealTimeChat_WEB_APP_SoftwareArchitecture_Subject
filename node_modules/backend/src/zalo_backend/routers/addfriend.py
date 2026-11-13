from fastapi import APIRouter, HTTPException, Depends, status
from zalo_backend.models.postgres.user import User
from zalo_backend.schemas.user import AddFriendRequest, AddFriendResponse, FriendRequestResponse
from sqlalchemy.orm import Session
from zalo_backend.database.l_postgres import get_db
from zalo_backend.user_service.service import get_user
from zalo_backend.user_service.addfr_service import create_friend_request
from zalo_backend.models.postgres.friendship import Friendship

# create a router for authentication routers
addfriend_router = APIRouter()

@addfriend_router.post("/add")
async def addfriend_request(request: AddFriendRequest, db: Session = Depends(get_db)):
    if not request.current_email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email empty"
        )
    # find current user
    curr_user = get_user(db=db, email=request.current_email)
    if curr_user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Current user not found"
        )
    # find target user
    targ_user = get_user(db=db, email=request.target_email)
    if targ_user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Target user not found"
        )
    # create friendship
    try:
        friendship_create = create_friend_request(
            db=db,
            requester_id=curr_user.id,
            addressee_id=targ_user.id
        )
    except ValueError as error:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(error)
        )
    # send real-time notification to target user
    await ws_manager.send_notification(
        targ_user.id,
        {
            "type": "friend_request",
            "request_id": friendship_create.id,
            "sender_id": curr_user.id,
            "sender_email": curr_user.email,
            "sender_username": curr_user.username,
            "message": f"{curr_user.username} sent you a friend request"
        }
    )
    # response of add friend request
    addfriend_res = AddFriendResponse(
        id=friendship_create.id,
        email=targ_user.email,
        username=targ_user.username,
        status=friendship_create.status
    )
    return {
        "data": addfriend_res,
        "error_code": None
    }

@addfriend_router.get("/request")
def get_addfriend_request(email: str, db: Session = Depends(get_db)):
    if not email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email empty"
        )
    # find current user
    curr_user = get_user(db=db, email=email)
    if curr_user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Current user not found"
        )
    # will be replace by data backend
    requests = (
        db.query(Friendship, User)
        .join(User, Friendship.requester_id == User.id)
        .filter(
            Friendship.addressee_id == curr_user.id,
            Friendship.status == "pending"
        )
        .all()
    )
    request_list = [
        FriendRequestResponse(
            id=friendship.id,
            email=sender.email,
            username=sender.username,
            status=friendship.status
        )
        for friendship, sender in requests
    ]

    return {
        "data": request_list,
        "error_code": None
    }