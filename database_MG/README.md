# Message service (Python / FastAPI)

Messages are stored in MongoDB. User IDs are positive PostgreSQL integers, not
MongoDB ObjectIds. Users are retrieved from the existing `database/main.py` API,
which calls `database/services.py`; no user data is duplicated in MongoDB.
Public contact responses never include passwords.

## Run

Requires Python 3.13+, uv, MongoDB and the PostgreSQL user service.
From the repository root, start the existing user service:

```powershell
python -m pip install -r database/requirements.txt
python -m uvicorn main:app --app-dir database --port 8000
```

In a separate terminal, configure and run the message service:

```powershell
cd database_MG
uv sync --extra dev
# Optional: override the Atlas URI configured in configuration.py:
# $env:MONGODB_URI = "mongodb://localhost:27017"
$env:MONGODB_DATABASE = "chat_db"
$env:USER_SERVICE_URL = "http://127.0.0.1:8000"
# Optional, required only when sending images:
$env:CLOUDINARY_URL = "cloudinary://api_key:api_secret@cloud_name"
uv run uvicorn main:app --host 127.0.0.1 --port 8001
```

Configuration uses defaults from `configuration.py`, with environment variable
overrides. If `MONGODB_URI` is unset, the configured Atlas URI is used.
`.env.example` is a template;
`.env` is not automatically loaded. MongoDB must be reachable at startup so the
service can create its conversation indexes. Open http://127.0.0.1:8001/docs.

## User Identification (No Authentication)

JWT is not used. Every HTTP endpoint requires an `X-User-ID` header containing
a positive PostgreSQL user ID. The service verifies that this user exists on
each request. Sender IDs cannot be supplied in request bodies.

Example request (user 1 sends a message to user 2):

```powershell
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8001/api/messages/send/2" -Headers @{"X-User-ID" = "1"} -ContentType "application/json" -Body '{"text":"Hello"}'
```

This is identification only, not authentication: anyone can supply another
user's ID to read messages, send as that user, or receive their realtime events.
Use this mode only for local/internal development, never on a public network.
The user API also returns password fields; keep it private. Add authentication
and address password handling before deploying the whole system publicly.

## HTTP API

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/api/messages/contacts` | PostgreSQL users except the supplied user |
| GET | `/api/messages/chats` | Unique users with messages in either direction |
| GET | `/api/messages/{id}` | Conversation with this user, oldest first |
| POST | `/api/messages/send/{id}` | Save and deliver a message; returns 201 |

Send body: `{"text": "Hello"}` or `{"image": "data:image/png;base64,..."}`,
or both. Text is trimmed and limited to 2,000 characters. Empty messages return
422, self-messaging returns 400, missing receivers return 404. Invalid IDs return
422. Responses retain `_id`, `senderId`, `receiverId`, `text`, `image`, `createdAt`
and `updatedAt` from the JS examples; user objects use the existing PostgreSQL
`id`, `name`, `email`, `phone_number` shape. Cloudinary uploads happen before
message insertion, and image strings are limited to 8,000,000 characters.
Unavailable databases/user service return 503; failed uploads return 502.

## Realtime And Limits

Connect a Socket.IO client to port 8001 using `auth: {user_id: 1}` (a positive
integer). The server checks that the supplied user exists and joins their room. Listen for
`newMessage` to receive the same object returned by the send endpoint. Clients
can impersonate any existing user in this unauthenticated mode. Offline users retrieve messages through the
history endpoint; realtime delivery failure does not undo a saved message.

The `limits` library caps HTTP requests at 120 per minute per client IP before user lookup.
This replaces basic rate limiting, not Arcjet's full bot/security features.
Use a reverse proxy to cap request size and configure trusted proxy headers
appropriately. Socket.IO accepts same-origin connections by default. Rooms and
rate limits are process-local: run one worker; configure a shared Socket.IO broker
and shared rate-limit storage before horizontal scaling. User existence is
checked at socket connection time only.

## Tests

```powershell
cd database_MG
uv run --extra dev pytest
```

Tests mock MongoDB, the user API, Cloudinary and realtime delivery. They cover
integer user IDs, password exclusion, sending without JWT, conversation
isolation, chat partners, validation, upstream failures and HTTP rate limiting.
They do not require live database credentials.

The Atlas credential is hard-coded in `configuration.py` for local development.
Do not share or commit this credential. Use environment variables and rotate the
credential before deployment; it may also exist in Git history.