from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class PublicUser(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int = Field(gt=0)
    name: Optional[str] = None
    email: Optional[str] = None
    phone_number: Optional[str] = None


class Message(BaseModel):
    id: str = Field(alias="_id")
    senderId: int
    receiverId: int
    text: Optional[str] = None
    image: Optional[str] = None
    createdAt: datetime
    updatedAt: datetime


def message_data(document):
    return Message.model_validate({**document, "_id": str(document["_id"])})
