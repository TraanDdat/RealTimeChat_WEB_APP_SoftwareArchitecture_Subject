import base64

import mongomock
import pytest
from fastapi import HTTPException

from zalo_backend.core.config import settings
from zalo_backend.message_service import s3_storage, service
from zalo_backend.models.mongodb.msg import MessageCreate


class FakeS3Client:
    def __init__(self):
        self.uploads = []

    def put_object(self, **kwargs):
        self.uploads.append(kwargs)

    def generate_presigned_url(self, operation, Params, ExpiresIn):
        assert operation == "get_object"
        assert Params["Bucket"] == "chat-images-test"
        assert ExpiresIn == 3600
        return f"https://s3.test/{Params['Key']}?signature=fake"

    def delete_object(self, **kwargs):
        return kwargs


def test_upload_stores_private_object_and_returns_key(monkeypatch):
    client = FakeS3Client()
    monkeypatch.setattr(settings, "S3_BUCKET", "chat-images-test")
    monkeypatch.setattr(s3_storage, "_client", lambda: client)
    image = b"\x89PNG\r\n\x1a\nsmall-test-image"
    data_url = "data:image/png;base64," + base64.b64encode(image).decode("ascii")

    key = s3_storage.store_image_data_url(data_url)

    assert key.startswith("chat-images/") and key.endswith(".png")
    assert client.uploads[0]["Body"] == image
    assert client.uploads[0]["ContentType"] == "image/png"
    assert client.uploads[0]["ServerSideEncryption"] == "AES256"
    assert "ACL" not in client.uploads[0]
    assert s3_storage.image_url(key) == f"https://s3.test/{key}?signature=fake"


def test_upload_rejects_non_image_payload(monkeypatch):
    monkeypatch.setattr(settings, "S3_BUCKET", "chat-images-test")

    with pytest.raises(HTTPException) as error:
        s3_storage.store_image_data_url("data:image/svg+xml;base64,PHN2Zz4=")

    assert error.value.status_code == 400


def test_legacy_cloudinary_urls_are_preserved():
    url = "https://res.cloudinary.com/demo/image/upload/photo.png"

    assert s3_storage.image_url(url) == url


def test_s3_client_ignores_machine_proxy_settings(monkeypatch):
    monkeypatch.setenv("AWS_ACCESS_KEY_ID", "test-access-key")
    monkeypatch.setenv("AWS_SECRET_ACCESS_KEY", "test-secret-key")
    monkeypatch.setenv("HTTP_PROXY", "http://127.0.0.1:9")
    monkeypatch.setenv("HTTPS_PROXY", "http://127.0.0.1:9")
    monkeypatch.setenv("NO_PROXY", "")

    client = s3_storage._client()

    assert client._endpoint.http_session._proxy_config._proxies == {}


def test_direct_message_persists_s3_key_and_returns_signed_url(monkeypatch):
    collection = mongomock.MongoClient().chat.messages
    key = "chat-images/direct.png"
    monkeypatch.setattr(service, "collection", collection)
    monkeypatch.setattr(service.user_client, "get_user", lambda user_id: object())
    monkeypatch.setattr(s3_storage, "store_image_data_url", lambda _: key)
    monkeypatch.setattr(s3_storage, "image_url", lambda image_ref: f"https://signed.test/{image_ref}")

    message = service.send_message(MessageCreate(image="data:image/png;base64,abc"), 1, 2)

    assert collection.find_one()["image"] == key
    assert message.image == f"https://signed.test/{key}"
    history = service.get_messages(1, 2)
    assert history[0].image == f"https://signed.test/{key}"