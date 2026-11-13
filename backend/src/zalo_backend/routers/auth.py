from fastapi import APIRouter, Depends
from zalo_backend.core.security import get_current_user

auth_router = APIRouter()

@auth_router.get("/check")
def check_auth(current_user = Depends(get_current_user)):
    return current_user