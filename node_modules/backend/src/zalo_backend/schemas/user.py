from typing import Optional
from pydantic import BaseModel, ConfigDict, EmailStr, Field

class UserBase(BaseModel):
    email: EmailStr
    password: str
    role: str

class LoginRequest(UserBase):
    pass

class LoginResponse(BaseModel):
    email: EmailStr
    username: str
    role: str
    access_token: str

class RegisterRequest(UserBase):
    username: str
    pass

class RegisterResponse(BaseModel):
    email: EmailStr
    username: str
    role: str

class ResetPwRequest(BaseModel):
    email: EmailStr
    new_password: str

class ResetPwResponse(BaseModel):
    email: EmailStr
    role: str

class AddFriendRequest(BaseModel):
    current_email: EmailStr
    target_email: EmailStr

class AddFriendResponse(BaseModel):
    id: int
    email: EmailStr
    username: str
    status: str

class FriendListRequest(BaseModel):
    email: EmailStr

class FriendListResponse(BaseModel):
    requests: list[AddFriendResponse]

class FriendRequestResponse(BaseModel):
    id: int
    email: EmailStr
    username: str
    status: str
	
class UpdateProfileRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    profilePic: str = Field(min_length=1, max_length=8_000_000)


class UserPublic(BaseModel):
    """User shape expected by the frontend (`_id`, `username`, `profilePic`)."""

    model_config = ConfigDict(populate_by_name=True)

    id: int = Field(gt=0, alias="_id")
    username: str
    email: str
    profilePic: Optional[str] = None

    @classmethod
    def from_user(cls, user):
        return cls(id=user.id, username=user.username, email=user.email, profilePic=user.profile_pic)
