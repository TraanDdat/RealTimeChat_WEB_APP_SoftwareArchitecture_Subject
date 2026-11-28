from fastapi import APIRouter, Depends, HTTPException, Query
from pymongo.errors import PyMongoError

from zalo_backend.core.security import get_current_user
from zalo_backend.models.postgres.user import User
from zalo_backend.user_service import activity_service

stats_router = APIRouter()


@stats_router.get("/me")
def get_my_stats(
    days: int = Query(default=7, ge=1, le=90),
    tz: str | None = Query(default=None, max_length=64),
    user: User = Depends(get_current_user),
):
    """Activity statistics of the current user over the last `days` days (local time zone `tz`)."""
    try:
        return activity_service.get_user_stats(user.id, days, tz)
    except PyMongoError as error:
        raise HTTPException(status_code=503, detail="Statistics storage unavailable") from error
