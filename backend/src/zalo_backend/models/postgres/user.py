from sqlalchemy import Column, Integer, String
from zalo_backend.database.l_postgres import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String(255), unique=True, index=True, nullable=False)
    username = Column(String(50), nullable=False)
    password = Column(String(255), nullable=False)
    role = Column(String(255))
    profile_pic = Column(String(500), nullable=True)