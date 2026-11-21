from fastapi import APIRouter, Depends, HTTPException, status, Response, Header
from zalo_backend.core.security import get_current_user
from zalo_backend.schemas.user import UserPublic
from typing import Literal, Optional
from zalo_backend.schemas.user import LoginRequest, UpdateProfileRequest
from sqlalchemy.orm import Session
from zalo_backend.database.l_postgres import get_db
from zalo_backend.user_service.service import get_user, update_userprofile
from zalo_backend.core.security import  create_access_token, set_auth_cookie, verify_password, clear_auth_cookie

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
    if not request.email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email empty"
        )
    print(f"request = {request}")
    user_from_db = get_user(
        db=db, email=request.email
    )

    if not user_from_db:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )
    
    if not verify_password(request.password, user_from_db.password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid password"
        )
    
    access_token: Optional[str] = None

    if client_type == "web":
        # React Web: HttpOnly Cookie
        print("debug")
        set_auth_cookie(response, user_from_db.id)

    elif client_type == "android":
        # Android: JWT Bearer Token
        access_token = create_access_token(user_from_db.id)
        
    print("Login request for email:", user_from_db.email)

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
    clear_auth_cookie(client_type, response)
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
        user_id = request.id,
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