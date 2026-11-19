from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, model_validator


class MessageCreate(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    text: Optional[str] = Field(default=None, max_length=2000)
    image: Optional[str] = Field(default=None, max_length=8_000_000)
    attachment: Optional[str] = Field(default=None, max_length=7_000_000)
    attachmentName: Optional[str] = Field(default=None, max_length=255)
    attachmentMimeType: Optional[str] = Field(default=None, max_length=127)

    @model_validator(mode="after")
    def require_content(self):
        has_attachment_metadata = self.attachmentName is not None or self.attachmentMimeType is not None
        if self.attachment and (not self.attachmentName or not self.attachmentMimeType):
            raise ValueError("Attachment name and media type are required.")
        if has_attachment_metadata and not self.attachment:
            raise ValueError("Attachment data is required.")
        if self.image and self.attachment:
            raise ValueError("Send either an image or an attachment, not both.")
        if not self.text and not self.image and not self.attachment:
            raise ValueError("Text, image, or attachment is required.")
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
    attachment: Optional[str] = None
    attachmentName: Optional[str] = None
    attachmentMimeType: Optional[str] = None
    attachmentSize: Optional[int] = None
    createdAt: datetime
    updatedAt: datetime


class GroupMessage(BaseModel):
    id: str = Field(alias="_id")
    groupId: int
    senderId: int
    senderUsername: str
    text: Optional[str] = None
    image: Optional[str] = None
    attachment: Optional[str] = None
    attachmentName: Optional[str] = None
    attachmentMimeType: Optional[str] = None
    attachmentSize: Optional[int] = None
    createdAt: datetime
    updatedAt: datetime


def message_data(document):
    return Message.model_validate({**document, "_id": str(document["_id"])})