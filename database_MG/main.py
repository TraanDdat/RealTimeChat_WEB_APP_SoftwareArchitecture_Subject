import logging
from contextlib import asynccontextmanager
from datetime import datetime, timezone

import cloudinary.uploader
import socketio
from fastapi import APIRouter, BackgroundTasks, Depends, FastAPI, Header, HTTPException, Path
from fastapi.concurrency import run_in_threadpool
from limits import parse
from limits.storage import MemoryStorage
from limits.strategies import MovingWindowRateLimiter
from pymongo import ASCENDING
from pymongo.errors import PyMongoError
from starlette.responses import JSONResponse

import user_service
from configuration import CLOUDINARY_URL, client, collection
from database.models import MessageCreate
from database.schemas import Message, PublicUser, message_data


logger = logging.getLogger(__name__)
sio = socketio.AsyncServer(async_mode="asgi")


@asynccontextmanager
async def lifespan(application):
    await run_in_threadpool(
        collection.create_index,
        [("senderId", ASCENDING), ("receiverId", ASCENDING), ("createdAt", ASCENDING)],
    )
    await run_in_threadpool(
        collection.create_index, [("receiverId", ASCENDING), ("senderId", ASCENDING)]
    )
    try:
        yield
    finally:
        client.close()


api = FastAPI(title="Chat Message API", lifespan=lifespan)
limiter = MovingWindowRateLimiter(MemoryStorage())
http_rate_limit = parse("120/minute")
router = APIRouter(prefix="/api/messages", tags=["messages"])


@api.middleware("http")
async def rate_limit_requests(request, call_next):
    address = request.client.host if request.client else "unknown"
    if not limiter.hit(http_rate_limit, address):
        return JSONResponse(
            status_code=429, content={"detail": "Too many requests."},
            headers={"Retry-After": "60"},
        )
    return await call_next(request)


@api.exception_handler(PyMongoError)
async def mongo_error_handler(request, error):
    logger.error("MongoDB operation failed", exc_info=error)
    return JSONResponse(status_code=503, content={"detail": "Message storage unavailable."})


def resolve_user(user_id: int) -> PublicUser:
    user = user_service.get_user(user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="User not found.")
    return user


def current_user(
    user_id: int = Header(alias="X-User-ID", gt=0),
) -> PublicUser:
    return resolve_user(user_id)


@sio.event
async def connect(sid, environ, auth):
    user_id = auth.get("user_id") if isinstance(auth, dict) else None
    if type(user_id) is not int or user_id <= 0:
        return False
    try:
        user = await run_in_threadpool(resolve_user, user_id)
    except HTTPException:
        return False
    await sio.enter_room(sid, str(user.id))


async def notify_receiver(receiver_id: int, message: Message):
    try:
        await sio.emit(
            "newMessage", message.model_dump(mode="json", by_alias=True),
            room=str(receiver_id),
        )
    except Exception:
        logger.exception("Realtime delivery failed for message %s", message.id)


@router.get("/contacts", response_model=list[PublicUser])
def get_all_contacts(user: PublicUser = Depends(current_user)):
    return [contact for contact in user_service.get_users() if contact.id != user.id]


@router.get("/chats", response_model=list[PublicUser])
def get_chat_partners(user: PublicUser = Depends(current_user)):
    partner_ids = set(collection.distinct("receiverId", {"senderId": user.id}))
    partner_ids.update(collection.distinct("senderId", {"receiverId": user.id}))
    partner_ids.discard(user.id)
    if not partner_ids:
        return []
    return [contact for contact in user_service.get_users() if contact.id in partner_ids]


@router.get("/{id}", response_model=list[Message])
def get_messages_by_user_id(
    id: int = Path(gt=0), user: PublicUser = Depends(current_user),
):
    messages = collection.find({"$or": [
        {"senderId": user.id, "receiverId": id},
        {"senderId": id, "receiverId": user.id},
    ]}).sort([("createdAt", ASCENDING), ("_id", ASCENDING)])
    return [message_data(document) for document in messages]


@router.post("/send/{id}", response_model=Message, status_code=201)
def send_message(
    data: MessageCreate, background_tasks: BackgroundTasks,
    id: int = Path(gt=0), user: PublicUser = Depends(current_user),
):
    if user.id == id:
        raise HTTPException(status_code=400, detail="Cannot send messages to yourself.")
    if user_service.get_user(id) is None:
        raise HTTPException(status_code=404, detail="Receiver not found.")

    image_url = None
    if data.image:
        if not CLOUDINARY_URL:
            raise HTTPException(status_code=503, detail="Image upload is not configured.")
        try:
            upload = cloudinary.uploader.upload(data.image, resource_type="image")
            image_url = upload["secure_url"]
        except Exception as error:
            logger.exception("Image upload failed")
            raise HTTPException(status_code=502, detail="Image upload failed.") from error

    now = datetime.now(timezone.utc)
    document = {
        "senderId": user.id, "receiverId": id, "text": data.text or None,
        "image": image_url, "createdAt": now, "updatedAt": now,
    }
    result = collection.insert_one(document)
    message = message_data({**document, "_id": result.inserted_id})
    background_tasks.add_task(notify_receiver, id, message)
    return message


api.include_router(router)
app = socketio.ASGIApp(sio, other_asgi_app=api)