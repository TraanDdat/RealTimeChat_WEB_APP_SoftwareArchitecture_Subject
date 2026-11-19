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
MAX_UPLOAD_BYTES = 5 * 1024 * 1024
_DATA_URL = re.compile(r"^data:([^;,]+);base64,([A-Za-z0-9+/]+={0,2})$")
_IMAGE_EXTENSIONS = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp", "image/gif": "gif"}
_IMAGE_SIGNATURES = {
    "image/jpeg": lambda data: data.startswith(b"\xff\xd8\xff"),
    "image/png": lambda data: data.startswith(b"\x89PNG\r\n\x1a\n"),
    "image/webp": lambda data: len(data) >= 12 and data[:4] == b"RIFF" and data[8:12] == b"WEBP",
    "image/gif": lambda data: data.startswith((b"GIF87a", b"GIF89a")),
}
_ATTACHMENT_TYPES = {
    "application/pdf": ("pdf", lambda data: data.startswith(b"%PDF-")),
    "text/plain": ("txt", lambda data: _is_plain_text(data)),
    "application/msword": ("doc", lambda data: data.startswith(b"\xd0\xcf\x11\xe0\xa1\xb1\x1a\xe1")),
    "application/vnd.ms-excel": ("xls", lambda data: data.startswith(b"\xd0\xcf\x11\xe0\xa1\xb1\x1a\xe1")),
    "application/vnd.ms-powerpoint": ("ppt", lambda data: data.startswith(b"\xd0\xcf\x11\xe0\xa1\xb1\x1a\xe1")),
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document": (
        "docx", lambda data: data.startswith(b"PK\x03\x04"),
    ),
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": (
        "xlsx", lambda data: data.startswith(b"PK\x03\x04"),
    ),
    "application/vnd.openxmlformats-officedocument.presentationml.presentation": (
        "pptx", lambda data: data.startswith(b"PK\x03\x04"),
    ),
    "video/mp4": ("mp4", lambda data: len(data) >= 8 and data[4:8] == b"ftyp"),
    "video/quicktime": ("mov", lambda data: len(data) >= 8 and data[4:8] == b"ftyp"),
    "video/webm": ("webm", lambda data: data.startswith(b"\x1a\x45\xdf\xa3")),
}


def _is_plain_text(data: bytes) -> bool:
    try:
        data.decode("utf-8")
    except UnicodeDecodeError:
        return False
    return True


def _client():
    return boto3.client(
        "s3",
        region_name=settings.AWS_REGION,
        config=Config(proxies={}),
    )


def store_image_data_url(data_url: str) -> str:
    if not settings.S3_BUCKET:
        raise HTTPException(status_code=503, detail="S3 image storage is not configured")

    match = _DATA_URL.fullmatch(data_url)
    if match is None:
        raise HTTPException(status_code=400, detail="Image must be a supported base64 data URL")

    content_type, encoded = match.groups()
    is_valid_image = _IMAGE_SIGNATURES.get(content_type)
    if is_valid_image is None:
        raise HTTPException(status_code=400, detail="Image must be a supported base64 data URL")
    try:
        image_bytes = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError) as error:
        raise HTTPException(status_code=400, detail="Invalid base64 image data") from error

    if not image_bytes:
        raise HTTPException(status_code=400, detail="Image is empty")
    if len(image_bytes) >= MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="File must be smaller than 5 MB")
    if not is_valid_image(image_bytes):
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


def store_attachment_data_url(
    data_url: str,
    filename: str,
    expected_content_type: str,
) -> tuple[str, int]:
    if not settings.S3_BUCKET:
        raise HTTPException(status_code=503, detail="S3 attachment storage is not configured")

    match = _DATA_URL.fullmatch(data_url)
    if match is None:
        raise HTTPException(status_code=400, detail="Attachment must be a supported base64 data URL")

    content_type, encoded = match.groups()
    if content_type != expected_content_type:
        raise HTTPException(status_code=400, detail="Attachment media type does not match its data")
    attachment_type = _ATTACHMENT_TYPES.get(content_type)
    if attachment_type is None:
        raise HTTPException(status_code=400, detail="Unsupported document or video type")
    extension, is_valid = attachment_type
    if filename.rsplit(".", 1)[-1].lower() != extension:
        raise HTTPException(status_code=400, detail="Filename extension does not match the file type")

    try:
        file_bytes = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError) as error:
        raise HTTPException(status_code=400, detail="Invalid base64 attachment data") from error

    if not file_bytes:
        raise HTTPException(status_code=400, detail="Attachment is empty")
    if len(file_bytes) >= MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="File must be smaller than 5 MB")
    if not is_valid(file_bytes):
        raise HTTPException(status_code=400, detail="File content does not match its media type")

    key = f"chat-attachments/{uuid.uuid4().hex}.{extension}"
    try:
        _client().put_object(
            Bucket=settings.S3_BUCKET,
            Key=key,
            Body=file_bytes,
            ContentType=content_type,
            ServerSideEncryption="AES256",
        )
    except (BotoCoreError, ClientError) as error:
        logger.exception("S3 attachment upload failed")
        raise HTTPException(status_code=502, detail="Attachment upload failed") from error
    return key, len(file_bytes)


def image_url(image_ref: str | None) -> str | None:
    return _object_url(image_ref, "image")


def attachment_url(attachment_ref: str | None) -> str | None:
    return _object_url(attachment_ref, "attachment")


def _object_url(object_ref: str | None, object_type: str) -> str | None:
    if not object_ref:
        return None
    if object_ref.startswith(("https://", "http://")):
        return object_ref
    if not settings.S3_BUCKET:
        raise HTTPException(status_code=503, detail=f"S3 {object_type} storage is not configured")
    try:
        return _client().generate_presigned_url(
            "get_object",
            Params={"Bucket": settings.S3_BUCKET, "Key": object_ref},
            ExpiresIn=settings.S3_PRESIGNED_URL_TTL_SECONDS,
        )
    except (BotoCoreError, ClientError) as error:
        logger.exception("Could not create S3 %s URL", object_type)
        raise HTTPException(status_code=502, detail=f"Could not access {object_type}") from error


def delete_image(image_key: str | None) -> None:
    if not image_key or not settings.S3_BUCKET:
        return
    try:
        _client().delete_object(Bucket=settings.S3_BUCKET, Key=image_key)
    except (BotoCoreError, ClientError):
        logger.exception("Could not clean up S3 image %s", image_key)