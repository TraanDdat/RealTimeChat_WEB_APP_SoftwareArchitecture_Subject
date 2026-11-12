from sqlalchemy.orm import Session
from sqlalchemy import or_, and_

from zalo_backend.models.postgres.user import User
from zalo_backend.models.postgres.friendship import Friendship


def create_friend_request(
    db: Session,
    requester_id: int,
    addressee_id: int
):
    # Không thể tự kết bạn với chính mình
    if requester_id == addressee_id:
        raise ValueError("Cannot add yourself")

    # Kiểm tra quan hệ đã tồn tại
    existing = check_friendship(
        db,
        requester_id,
        addressee_id
    )

    if existing:
        raise ValueError("Friendship request already exists")

    friendship = Friendship(
        requester_id=requester_id,
        addressee_id=addressee_id,
        status="pending"
    )

    db.add(friendship)
    db.commit()
    db.refresh(friendship)

    return friendship


def check_friendship(
    db: Session,
    user_id_1: int,
    user_id_2: int
):
    return db.query(Friendship).filter(
        or_(
            and_(
                Friendship.requester_id == user_id_1,
                Friendship.addressee_id == user_id_2
            ),
            and_(
                Friendship.requester_id == user_id_2,
                Friendship.addressee_id == user_id_1
            )
        )
    ).first()


def get_friend_requests(db: Session, user_id: int):
    return (
        db.query(Friendship, User)
        .join(User, Friendship.requester_id == User.id)
        .filter(
            Friendship.addressee_id == user_id,
            Friendship.status == "pending"
        )
        .all()
    )


def update_friendship_status(
    db: Session,
    friendship_id: int,
    user_id: int,
    new_status: str
):
    if new_status not in ["accepted", "rejected"]:
        raise ValueError("Invalid friendship status")

    friendship = db.query(Friendship).filter(
        Friendship.id == friendship_id,
        Friendship.addressee_id == user_id,
        Friendship.status == "pending"
    ).first()

    if friendship is None:
        return None

    friendship.status = new_status

    db.commit()
    db.refresh(friendship)

    return friendship


def get_friends(db: Session, user_id: int):
    return (
        db.query(Friendship, User)
        .join(
            User,
            or_(
                and_(
                    Friendship.requester_id == user_id,
                    User.id == Friendship.addressee_id
                ),
                and_(
                    Friendship.addressee_id == user_id,
                    User.id == Friendship.requester_id
                )
            )
        )
        .filter(Friendship.status == "accepted")
        .all()
    )