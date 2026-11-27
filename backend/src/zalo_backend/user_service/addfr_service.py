from sqlalchemy.orm import Session
from sqlalchemy import or_, and_, func

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

    if existing and existing.status == "rejected":
        # A rejected request may be sent again (by either user). Reuse the row
        # because the DB allows only one friendship row per pair of users.
        existing.requester_id = requester_id
        existing.addressee_id = addressee_id
        existing.status = "pending"
        existing.updated_at = func.now()
        db.commit()
        db.refresh(existing)
        return existing

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


def get_pending_friend_requests(db: Session, user_id: int):
    return (
        db.query(Friendship, User)
        .join(User, Friendship.requester_id == User.id)
        .filter(
            Friendship.addressee_id == user_id,
            Friendship.status == "pending",
        )
        .all()
    )


def get_sent_friend_requests(db: Session, user_id: int):
    """Requests this user sent, with their current status, newest change first."""
    return (
        db.query(Friendship, User)
        .join(User, Friendship.addressee_id == User.id)
        .filter(Friendship.requester_id == user_id)
        .order_by(Friendship.updated_at.desc(), Friendship.id.desc())
        .all()
    )


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

    # Keep rejected requests (instead of deleting them) so the requester can
    # see the outcome via get_sent_friend_requests.
    friendship.status = new_status
    friendship.updated_at = func.now()

    db.commit()
    db.refresh(friendship)

    return friendship


def delete_friendship(db: Session, user_id_1: int, user_id_2: int) -> bool:
    friendship = check_friendship(db, user_id_1, user_id_2)
    if friendship is None or friendship.status != "accepted":
        return False

    db.delete(friendship)
    db.commit()
    return True

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

def get_friendship_users(db: Session, friendship_id: int):
    friendship = (
        db.query(Friendship)
        .filter(Friendship.id == friendship_id)
        .first()
    )

    if friendship is None:
        return None

    return {
        "requester_id": friendship.requester_id,
        "addressee_id": friendship.addressee_id
    }