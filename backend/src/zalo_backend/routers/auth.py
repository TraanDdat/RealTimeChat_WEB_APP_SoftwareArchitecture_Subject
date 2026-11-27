from typing import Literal

from fastapi import APIRouter, Depends, Header, HTTPException, Response, status
from sqlalchemy.orm import Session

from zalo_backend.core.security import (
    clear_auth_cookie,
    create_access_token,
    get_current_user,
    set_auth_cookie,
    verify_password,
)
from zalo_backend.database.l_postgres import get_db
from zalo_backend.schemas.user import LoginRequest, UpdateProfileRequest, UserPublic
from zalo_backend.user_service.service import get_user, update_userprofile

auth_router = APIRouter()

@auth_router.get("/check", response_model=UserPublic)
def check_auth(current_user = Depends(get_current_user)):
    return UserPublic.from_user(current_user)

@auth_router.post("/login")
def login_request(
    request: LoginRequest, 
    response: Response, 
    client_type: Literal["web", "android"] = Header(
        default="web",
        alias="X-Client-Type"
    ),
    db: Session = Depends(get_db)):
    user_from_db = get_user(
        db=db, email=request.email
    )

    if user_from_db is None or not verify_password(request.password, user_from_db.password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password"
        )
    
    access_token = None

    if client_type == "web":
        # React Web: HttpOnly Cookie
        set_auth_cookie(response, user_from_db.id)

    elif client_type == "android":
        # Android: JWT Bearer Token
        access_token = create_access_token(user_from_db.id)
        
    return {
        "data": {
            "id": user_from_db.id,
            "email": user_from_db.email,
            "username": user_from_db.username,
            "access_token": access_token,
            "profilePic": user_from_db.profile_pic if hasattr(user_from_db, "profile_pic") else None
        },
        "error_code": None
    }

@auth_router.post("/logout")
def logout(
        response: Response, 
        current_user=Depends(get_current_user),
        client_type: Literal["web", "android"] = Header(default="web", alias="X-Client-Type")
    ):
    clear_auth_cookie(response, client_type)
    return {
        "error_code": None
    }

@auth_router.patch("/update-profile")
def update_profile(
    request: UpdateProfileRequest,
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user)
):
    new_user = update_userprofile(
        db =  db,
        user_name = request.username,
        user_id = current_user.id,
        profile_pic = request.profilePic
    )
    if not new_user:
        raise HTTPException(
            status_code=404,
            detail="User not found"
        )
    return {
        "id": new_user.id,
        "username": new_user.username,
        "profilePic": new_user.profile_pic
    }