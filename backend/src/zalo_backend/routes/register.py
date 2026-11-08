from fastapi import APIRouter, HTTPException, Depends, status
from zalo_backend.schemas.user import RegisterRequest, RegisterResponse
from sqlalchemy.orm import Session
from zalo_backend.database.l_postgres import get_db
from zalo_backend.user_service.service import create_user, get_user

# create a router for authentication routes
register_router = APIRouter()

@register_router.post("/register")
def register_request(request: RegisterRequest, db: Session = Depends(get_db)):
    err_code = None
    if not request.email:
        raise HTTPException(
            status=status.HTTP_400_BAD_REQUEST,
            detail="Email empty"
        )

    user_from_db = get_user(
        db=db, email=request.email
    )

    if not user_from_db:
        create_user(db, request)
        register_res = RegisterResponse(
            email=request.email,
            role=request.role
        )

        print("Register request for email:", request.email, "with role:", request.role)
    else:
        print("Email has already been registered:", request.email)
        raise HTTPException(
            status=status.HTTP_400_BAD_REQUEST,
            detail="User already exists"
        )

    return {
        "data": register_res,
        "error_code": None
    }