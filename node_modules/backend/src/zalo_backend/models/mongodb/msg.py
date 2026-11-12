from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, model_validator


class MessageCreate(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    text: Optional[str] = Field(default=None, max_length=2000)
    image: Optional[str] = Field(default=None, max_length=8_000_000)

    @model_validator(mode="after")
    def require_content(self):
        if not self.text and not self.image:
            raise ValueError("Text or image is required.")
        return self


class PublicUser(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int = Field(gt=0)
    email: Optional[str] = None


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