from fastapi import APIRouter, HTTPException, Depends
from zalo_backend.models.postgres.user import User
from sqlalchemy.orm import Session
from zalo_backend.database.l_postgres import get_db
from zalo_backend.user_service.service import _find_user

resetpw_router = APIRouter()

@resetpw_router.post("/resetpw")
def resetpw_request(request: User, db: Session = Depends(get_db)):
    try:
        if _find_user(db=db, email=request.email) is None:
            raise HTTPException(
                status_code=404,
                detail="User not found"
            )
        return {
            "error_code": None
        }
    except:
        return {
            "error_code": "USERS_NOT_FOUND"
        }