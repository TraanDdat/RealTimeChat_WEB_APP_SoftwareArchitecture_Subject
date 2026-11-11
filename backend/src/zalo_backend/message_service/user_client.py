from zalo_backend.database.l_postgres import SessionLocal
from zalo_backend.schemas.user import UserPublic
from zalo_backend.user_service import service as user_service


def get_user(user_id: int):
    with SessionLocal() as db:
        user = user_service.get_user(db, user_id=user_id)
        return UserPublic.from_user(user) if user else None


def get_users():
    with SessionLocal() as db:
        return [UserPublic.from_user(user) for user in user_service.get_users(db)]