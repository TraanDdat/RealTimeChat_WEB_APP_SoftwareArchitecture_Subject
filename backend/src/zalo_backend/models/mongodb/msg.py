from datetime import datetime, timezone
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, model_validator


class MessageCreate(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    text: Optional[str] = Field(default=None, max_length=2000)
    image: Optional[str] = Field(default=None, max_length=8_000_000)
    emotion: Optional[str] = None
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
        if not self.text and not self.image and not self.attachment and not self.emotion:
            raise ValueError("Message content is required, should not null.")
        return self


class ForwardRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    messageId: str = Field(min_length=1, max_length=64)
    sourceGroupId: Optional[int] = Field(default=None, gt=0)
    receiverIds: list[int] = Field(default_factory=list, max_length=50)
    groupIds: list[int] = Field(default_factory=list, max_length=50)

    @model_validator(mode="after")
    def require_target(self):
        if not self.receiverIds and not self.groupIds:
            raise ValueError("Choose at least one conversation to forward to.")
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
    emotion: Optional[str] = None
    attachment: Optional[str] = None
    attachmentName: Optional[str] = None
    attachmentMimeType: Optional[str] = None
    attachmentSize: Optional[int] = None
    isRecalled: bool = False
    isForwarded: bool = False
    createdAt: datetime
    updatedAt: datetime


class GroupMessage(BaseModel):
    id: str = Field(alias="_id")
    groupId: int
    senderId: int
    senderUsername: str
    text: Optional[str] = None
    image: Optional[str] = None
    emotion: Optional[str] = None
    attachment: Optional[str] = None
    attachmentName: Optional[str] = None
    attachmentMimeType: Optional[str] = None
    attachmentSize: Optional[int] = None
    isRecalled: bool = False
    isForwarded: bool = False
    createdAt: datetime
    updatedAt: datetime


class ForwardResponse(BaseModel):
    messages: list[Message]
    groupMessages: list[GroupMessage]


def recalled_message_fields() -> dict:
    """Fields set on a recalled message: content is wiped, the record stays as a placeholder."""
    return {
        "isRecalled": True,
        "text": None,
        "image": None,
        "emotion": None,
        "attachment": None,
        "attachmentName": None,
        "attachmentMimeType": None,
        "attachmentSize": None,
        "updatedAt": datetime.now(timezone.utc),
    }


def message_data(document):
    return Message.model_validate({**document, "_id": str(document["_id"])})