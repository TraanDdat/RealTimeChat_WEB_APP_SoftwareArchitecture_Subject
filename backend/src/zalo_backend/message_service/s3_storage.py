import base64
import binascii
import logging
import re
import uuid

import boto3
from botocore.config import Config
from botocore.exceptions import BotoCoreError, ClientError
from fastapi import HTTPException

from zalo_backend.core.config import settings


logger = logging.getLogger(__name__)
MAX_IMAGE_BYTES = 5 * 1024 * 1024
_IMAGE_DATA_URL = re.compile(r"^data:(image/(?:jpeg|png|webp|gif));base64,([A-Za-z0-9+/]+={0,2})$")
_IMAGE_EXTENSIONS = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp", "image/gif": "gif"}
_IMAGE_SIGNATURES = {
    "image/jpeg": lambda data: data.startswith(b"\xff\xd8\xff"),
    "image/png": lambda data: data.startswith(b"\x89PNG\r\n\x1a\n"),
    "image/webp": lambda data: len(data) >= 12 and data[:4] == b"RIFF" and data[8:12] == b"WEBP",
    "image/gif": lambda data: data.startswith((b"GIF87a", b"GIF89a")),
}


def _client():
    return boto3.client(
        "s3",
        region_name=settings.AWS_REGION,
        config=Config(proxies={}),
    )


def store_image_data_url(data_url: str) -> str:
    if not settings.S3_BUCKET:
        raise HTTPException(status_code=503, detail="S3 image storage is not configured")

    match = _IMAGE_DATA_URL.fullmatch(data_url)
    if match is None:
        raise HTTPException(status_code=400, detail="Image must be a supported base64 data URL")

    content_type, encoded = match.groups()
    try:
        image_bytes = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError) as error:
        raise HTTPException(status_code=400, detail="Invalid base64 image data") from error

    if not image_bytes:
        raise HTTPException(status_code=400, detail="Image is empty")
    if len(image_bytes) > MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail="Image exceeds the 5 MB limit")
    if not _IMAGE_SIGNATURES[content_type](image_bytes):
        raise HTTPException(status_code=400, detail="Image content does not match its media type")

    key = f"chat-images/{uuid.uuid4().hex}.{_IMAGE_EXTENSIONS[content_type]}"
    try:
        _client().put_object(
            Bucket=settings.S3_BUCKET,
            Key=key,
            Body=image_bytes,
            ContentType=content_type,
            ServerSideEncryption="AES256",
        )
    except (BotoCoreError, ClientError) as error:
        logger.exception("S3 image upload failed")
        raise HTTPException(status_code=502, detail="Image upload failed") from error
    return key


def image_url(image_ref: str | None) -> str | None:
    if not image_ref:
        return None
    if image_ref.startswith(("https://", "http://")):
        return image_ref
    if not settings.S3_BUCKET:
        raise HTTPException(status_code=503, detail="S3 image storage is not configured")
    try:
        return _client().generate_presigned_url(
            "get_object",
            Params={"Bucket": settings.S3_BUCKET, "Key": image_ref},
            ExpiresIn=settings.S3_PRESIGNED_URL_TTL_SECONDS,
        )
    except (BotoCoreError, ClientError) as error:
        logger.exception("Could not create S3 image URL")
        raise HTTPException(status_code=502, detail="Could not access image") from error


def delete_image(image_key: str | None) -> None:
    if not image_key or not settings.S3_BUCKET:
        return
    try:
        _client().delete_object(Bucket=settings.S3_BUCKET, Key=image_key)
    except (BotoCoreError, ClientError):
        logger.exception("Could not clean up S3 image %s", image_key)