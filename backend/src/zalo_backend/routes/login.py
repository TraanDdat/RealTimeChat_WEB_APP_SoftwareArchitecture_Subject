from fastapi import APIRouter, Depends, HTTPException, Response, status
from fastapi.concurrency import run_in_threadpool
from sqlalchemy.orm import Session

from zalo_backend.core.security import clear_auth_cookie, set_auth_cookie, verify_password
from zalo_backend.database.l_postgres import get_db
from zalo_backend.message_service.dependencies import current_user
from zalo_backend.message_service.service import upload_image
from zalo_backend.schemas.user import LoginRequest, UpdateProfileRequest, UserPublic
from zalo_backend.user_service.service import get_user, update_profile_pic

# create a router for authentication routes
login_router = APIRouter()


@login_router.post("/login", response_model=UserPublic)
def login_request(request: LoginRequest, response: Response, db: Session = Depends(get_db)):
    user = get_user(db=db, email=request.email)
    if not user or not verify_password(request.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid email or password")

    set_auth_cookie(response, user.id)
    return UserPublic.from_user(user)


@login_router.post("/logout")
def logout_request(response: Response):
    clear_auth_cookie(response)
    return {"message": "Logged out successfully"}


@login_router.get("/check", response_model=UserPublic)
def check_auth(user: UserPublic = Depends(current_user)):
    return user


@login_router.put("/update-profile", response_model=UserPublic)
def update_profile(
    request: UpdateProfileRequest,
    user: UserPublic = Depends(current_user),
    db: Session = Depends(get_db),
):
    url = upload_image(request.profilePic)
    return UserPublic.from_user(update_profile_pic(db, user.id, url))