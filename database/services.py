from models import User
from sqlalchemy.orm import Session
from schemas import UserCreate


def create_user(db: Session, data: UserCreate):
    user_instance = User(**data.model_dump())
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
    return _find_user(db, user_id, email)

def update_user(
    db: Session,
    user: UserCreate,
    user_id: int | None = None,
    email: str | None = None,
):
    user_queryset = _find_user(db, user_id, email)
    if user_queryset:
        for key,value in user.model_dump().items():
            setattr(user_queryset, key, value)
        db.commit()
        db.refresh(user_queryset)
    return user_queryset

def remove_user(db: Session, user_id: int | None = None, email: str | None = None):
    user_queryset = _find_user(db, user_id, email)
    if user_queryset:
        db.delete(user_queryset)
        db.commit()
        return True
    return False
