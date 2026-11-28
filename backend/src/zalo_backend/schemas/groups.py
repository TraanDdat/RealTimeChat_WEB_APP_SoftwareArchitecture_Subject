from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class GroupCreateRequest(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    member_ids: list[int] = Field(default_factory=list)


class GroupMemberAddRequest(BaseModel):
    user_id: int = Field(gt=0)
    role: Literal["member", "admin"] = "member"

class GroupMemberRemoveRequest(BaseModel):
    user_id: int = Field(gt=0)
    role: Literal["member"] = "member"

class GroupMemberLeaveRequest(BaseModel):
    user_id: int = Field(gt=0)
    role: Literal["member"] = "member"

class GroupMemberRoleUpdate(BaseModel):
    role: Literal["member", "admin"]


class GroupMemberResponse(BaseModel):
    user_id: int
    username: str
    email: str
    role: str


class GroupResponse(BaseModel):
    id: int
    name: str
    created_by: int
    created_at: datetime
    members: list[GroupMemberResponse]