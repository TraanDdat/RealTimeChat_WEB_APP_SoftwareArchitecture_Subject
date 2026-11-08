from pydantic import BaseModel, EmailStr

class UserBase(BaseModel):
    email: EmailStr
    password: str
    role: str

class LoginRequest(UserBase):
    pass

class LoginResponse(BaseModel):
    email: EmailStr
    role: str

class RegisterRequest(UserBase):
    pass

class RegisterResponse(BaseModel):
    email: EmailStr
    role: str

class ResetPwRequest(BaseModel):
    email: EmailStr
    new_password: str

class ResetPwResponse(BaseModel):
    email: EmailStr
    role: str