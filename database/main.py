from fastapi import FastAPI, Depends, HTTPException
import services, models, schemas
from db import get_db, engine
from sqlalchemy.orm import Session

app = FastAPI()

@app.get("/users/", response_model=list[schemas.User])
def get_all_users(db: Session = Depends(get_db)):
    return services.get_users(db)

@app.get("/users/{id}", response_model=schemas.User)
def get_user_by_id(id:int, db: Session = Depends(get_db)):
    user_queryset = services.get_user(db,id)
    if user_queryset:
        return user_queryset
    raise HTTPException(status_code=404, detail="Invalid User id Provided")

@app.get("/users/email/{email}", response_model=schemas.User)
def get_user_by_email(email: str, db: Session = Depends(get_db)):
    user_queryset = services.get_user(db, email=email)
    if user_queryset:
        return user_queryset
    raise HTTPException(status_code=404, detail="User not found")



@app.post("/users", response_model = schemas.User)
def create_new_user(user: schemas.UserCreate, db: Session = Depends(get_db)):
    return services.create_user(db, user)

@app.put("/users/{id}", response_model=schemas.User)
def update_user(user: schemas.UserCreate, id:int, db: Session = Depends(get_db)):
    db_update = services.update_user(db, user, id)
    if not db_update:
        raise HTTPException(status_code=404, detail="User not found")
    return db_update

@app.put("/users/email/{email}", response_model=schemas.User)
def update_user_by_email(user: schemas.UserCreate, email: str, db: Session = Depends(get_db)):
    db_update = services.update_user(db, user, email=email)
    if not db_update:
        raise HTTPException(status_code=404, detail="User not found")
    return db_update

@app.delete("/users/{id}", status_code=204)
def remove_user(id: int, db: Session = Depends(get_db)):
    if not services.remove_user(db, id):
        raise HTTPException(status_code=404, detail="User not found")

@app.delete("/users/email/{email}", status_code=204)
def remove_user_by_email(email: str, db: Session = Depends(get_db)):
    if not services.remove_user(db, email=email):
        raise HTTPException(status_code=404, detail="User not found")