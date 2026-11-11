from fastapi import APIRouter, HTTPException, Depends, Response, status
from sqlalchemy.orm import Session

from zalo_backend.core.security import set_auth_cookie
from zalo_backend.database.l_postgres import get_db
from zalo_backend.schemas.user import RegisterRequest, UserPublic
from zalo_backend.user_service.service import create_user, get_user

# create a router for authentication routes
register_router = APIRouter()


@register_router.post("/signup", status_code=status.HTTP_201_CREATED, response_model=UserPublic)
def register_request(request: RegisterRequest, response: Response, db: Session = Depends(get_db)):
    if get_user(db=db, email=request.email):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Email already exists")

    user = create_user(db, request)
    set_auth_cookie(response, user.id)
    return UserPublic.from_user(user)