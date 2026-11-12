from sqlalchemy.orm import Session
from zalo_backend.core.security import hash_password

from zalo_backend.models.postgres.user import User
from zalo_backend.schemas.user import RegisterRequest


def create_user(db: Session, data: RegisterRequest):
    user_instance = User(
        email=data.email,
        username=data.username,
        password=hash_password(data.password),
        role=data.role
    )
    
    db.add(user_instance)
    db.commit()
    db.refresh(user_instance)
    return user_instance


def get_users(db: Session):
    return db.query(User).all()


def _find_user(db: Session, user_id: int | None = None, email: str | None = None):
    if (user_id is None) == (email is None):
        raise ValueError("Provide either user_id or email")

    query = db.query(User)
    if user_id is not None:
        query = query.filter(User.id == user_id)
    else:
        query = query.filter(User.email == email)
    return query.first()


def get_user(db: Session, user_id: int | None = None, email: str | None = None):
    if not _find_user(db, user_id, email):
        return None
    else:
        return _find_user(db, user_id, email)


def update_user(
    db: Session,
    user: RegisterRequest,
    user_id: int | None = None,
    email: str | None = None,
):
    user_queryset = _find_user(db, user_id, email)
    if user_queryset:
        for key, value in user.model_dump().items():
            setattr(user_queryset, key, value)
        db.commit()
        db.refresh(user_queryset)
    return user_queryset


def update_profile_pic(db: Session, user_id: int, profile_pic: str):
    user = _find_user(db, user_id=user_id)
    if user:
        user.profile_pic = profile_pic
        db.commit()
        db.refresh(user)
    return user


def remove_user(db: Session, user_id: int | None = None, email: str | None = None):
    user_queryset = _find_user(db, user_id, email)
    if user_queryset:
        db.delete(user_queryset)
        db.commit()
        return True
    return False

def get_user_by_id(db: Session, user_id: int):
    return _find_user(db, user_id=user_id)