from sqlalchemy import create_engine, text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

from zalo_backend.core.config import settings


engine = create_engine(settings.DATABASE_URL)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def check_postgres():
    with engine.connect() as connection:
        connection.execute(text("SELECT 1"))

def create_tables():
    Base.metadata.create_all(bind=engine)

def close_postgres():
    engine.dispose()