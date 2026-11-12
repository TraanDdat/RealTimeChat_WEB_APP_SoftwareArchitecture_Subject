from fastapi import APIRouter, HTTPException, Depends, status, Response
from zalo_backend.schemas.user import LoginRequest, LoginResponse
from sqlalchemy.orm import Session
from zalo_backend.database.l_postgres import get_db
from zalo_backend.user_service.service import get_user
from zalo_backend.core.security import  set_auth_cookie, verify_password

# create a router for authentication routers
login_router = APIRouter()

@login_router.post("/login")
def login_request(request: LoginRequest, response: Response, db: Session = Depends(get_db)):
    if not request.email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email empty"
        )

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
    
    set_auth_cookie(response, user_from_db.id)
    login_res = LoginResponse(
        email=user_from_db.email,
        role=user_from_db.role,
        username=user_from_db.username,
        access_token=""
    )
    print("Login request for email:", user_from_db.email, "with role:", user_from_db.role)

    return {
        "data": login_res,
        "error_code": None
    }