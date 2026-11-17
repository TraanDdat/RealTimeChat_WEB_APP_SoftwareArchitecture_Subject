from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.sql import func

from zalo_backend.database.l_postgres import Base


class GroupChat(Base):
    __tablename__ = "group_chats"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False)
    created_by = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)


class GroupMember(Base):
    __tablename__ = "group_members"
    __table_args__ = (UniqueConstraint("group_id", "user_id", name="uq_group_member"),)

    id = Column(Integer, primary_key=True, index=True)
    group_id = Column(Integer, ForeignKey("group_chats.id", ondelete="CASCADE"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    role = Column(String(16), nullable=False, default="member")
    added_by = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    joined_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)