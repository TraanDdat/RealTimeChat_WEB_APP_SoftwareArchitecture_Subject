from fastapi import APIRouter, Depends
from zalo_backend.core.security import get_current_user
from zalo_backend.schemas.user import UserPublic

auth_router = APIRouter()

@auth_router.get("/check", response_model=UserPublic)
def check_auth(current_user = Depends(get_current_user)):
    return UserPublic.from_user(current_user)