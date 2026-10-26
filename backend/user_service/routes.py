from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from . import schemas, service
from .database import get_db


router = APIRouter(prefix="/users", tags=["users"])


@router.get("/", response_model=list[schemas.User])
def get_all_users(db: Session = Depends(get_db)):
    return service.get_users(db)


@router.get("/{id}", response_model=schemas.User)
def get_user_by_id(id: int, db: Session = Depends(get_db)):
    user_queryset = service.get_user(db, id)
    if user_queryset:
        return user_queryset
    raise HTTPException(status_code=404, detail="Invalid User id Provided")


@router.get("/email/{email}", response_model=schemas.User)
def get_user_by_email(email: str, db: Session = Depends(get_db)):
    user_queryset = service.get_user(db, email=email)
    if user_queryset:
        return user_queryset
    raise HTTPException(status_code=404, detail="User not found")


@router.post("", response_model=schemas.User)
def create_new_user(user: schemas.UserCreate, db: Session = Depends(get_db)):
    return service.create_user(db, user)


@router.put("/{id}", response_model=schemas.User)
def update_user(user: schemas.UserCreate, id: int, db: Session = Depends(get_db)):
    db_update = service.update_user(db, user, id)
    if not db_update:
        raise HTTPException(status_code=404, detail="User not found")
    return db_update


@router.put("/email/{email}", response_model=schemas.User)
def update_user_by_email(
    user: schemas.UserCreate, email: str, db: Session = Depends(get_db),
):
    db_update = service.update_user(db, user, email=email)
    if not db_update:
        raise HTTPException(status_code=404, detail="User not found")
    return db_update


@router.delete("/{id}", status_code=204)
def remove_user(id: int, db: Session = Depends(get_db)):
    if not service.remove_user(db, id):
        raise HTTPException(status_code=404, detail="User not found")


@router.delete("/email/{email}", status_code=204)
def remove_user_by_email(email: str, db: Session = Depends(get_db)):
    if not service.remove_user(db, email=email):
        raise HTTPException(status_code=404, detail="User not found")