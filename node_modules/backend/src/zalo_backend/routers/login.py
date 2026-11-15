from fastapi import APIRouter, HTTPException, Depends, status, Response, Header
from typing import Literal, Optional
from zalo_backend.schemas.user import LoginRequest, LoginResponse
from sqlalchemy.orm import Session
from zalo_backend.database.l_postgres import get_db
from zalo_backend.user_service.service import get_user
from zalo_backend.core.security import  create_access_token, set_auth_cookie, verify_password

# create a router for authentication routers
login_router = APIRouter()

@login_router.post("/login")
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