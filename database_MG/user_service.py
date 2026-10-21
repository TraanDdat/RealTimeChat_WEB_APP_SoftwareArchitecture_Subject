import httpx
from fastapi import HTTPException
from pydantic import ValidationError

from configuration import USER_SERVICE_URL
from database.schemas import PublicUser


def _request(path: str, allow_missing: bool = False):
    try:
        response = httpx.get(f"{USER_SERVICE_URL}{path}", timeout=5.0)
        if allow_missing and response.status_code == 404:
            return None
        response.raise_for_status()
        return response.json()
    except (httpx.HTTPError, ValueError) as error:
        raise HTTPException(status_code=503, detail="User service unavailable.") from error


def get_user(user_id: int):
    data = _request(f"/users/{user_id}", allow_missing=True)
    if data is None:
        return None
    try:
        return PublicUser.model_validate(data)
    except ValidationError as error:
        raise HTTPException(status_code=502, detail="Invalid user service response.") from error


def get_users():
    data = _request("/users/")
    if not isinstance(data, list):
        raise HTTPException(status_code=502, detail="Invalid user service response.")
    try:
        return [PublicUser.model_validate(user) for user in data]
    except ValidationError as error:
        raise HTTPException(status_code=502, detail="Invalid user service response.") from error