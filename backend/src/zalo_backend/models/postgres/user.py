from sqlalchemy import Column, Integer, String
from zalo_backend.database.l_postgres import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    # display name (frontend "fullName"), so not unique
    username = Column(String(50), nullable=False)
    email = Column(String(255), unique=True, index=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    profile_pic = Column(String(500), nullable=True)