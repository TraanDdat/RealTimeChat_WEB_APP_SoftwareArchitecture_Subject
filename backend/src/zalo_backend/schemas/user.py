from typing import Optional

from pydantic import BaseModel, ConfigDict, EmailStr, Field


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1)


class RegisterRequest(BaseModel):
    fullName: str = Field(min_length=1, max_length=50)
    email: EmailStr
    password: str = Field(min_length=6, max_length=128)


class UpdateProfileRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    profilePic: str = Field(min_length=1, max_length=8_000_000)


class UserPublic(BaseModel):
    """User shape expected by the frontend (`_id`, `fullName`, `profilePic`)."""

    model_config = ConfigDict(populate_by_name=True)

    id: int = Field(gt=0, alias="_id")
    fullName: str
    email: str
    profilePic: Optional[str] = None

    @classmethod
    def from_user(cls, user):
        return cls(id=user.id, fullName=user.username, email=user.email, profilePic=user.profile_pic)


class ResetPwRequest(BaseModel):
    email: EmailStr
    new_password: str

class ResetPwResponse(BaseModel):
    email: EmailStr
    role: str