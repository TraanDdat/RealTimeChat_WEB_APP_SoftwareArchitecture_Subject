# Realtime Chat Backend

Two Python service packages, organized by business capability rather than database
vendor. The existing HTTP paths, payloads, database schema and Socket.IO events
are preserved. Both services run from the repository root; do not change into a
service directory or use bare imports such as `import main`.

## Structure

```text
backend/
		__init__.py
		user_service/              # Users; PostgreSQL via SQLAlchemy
				__init__.py
				main.py                # FastAPI application and router registration
				routers.py              # HTTP endpoints
				service.py             # User CRUD operations
				schemas.py             # Request/response contracts
				models.py              # SQLAlchemy table mappings
				database.py            # Engine, session dependency, table initialization
				config.py              # PostgreSQL environment configuration
		message_service/           # Messages; MongoDB via PyMongo
				__init__.py
				main.py                # ASGI app, lifespan, middleware, error handlers
				routers.py              # HTTP endpoints
				service.py             # Conversations, persistence and image uploads
				schemas.py             # Request/response contracts and document mapping
				database.py            # MongoDB client, collection and indexes
				config.py              # MongoDB, user API and Cloudinary configuration
				dependencies.py        # Current-user identification
				user_client.py         # HTTP adapter to the user service
				realtime.py            # Socket.IO connections, rooms and notifications
tests/
		test_users.py
		test_messages.py
.env.example
pyproject.toml                 # Shared dependencies and test configuration
uv.lock                       # Reproducible dependency resolution
```

`database/` has become `backend/user_service/`; `database_MG/` has become
`backend/message_service/`. MongoDB's old `database/models.py` only defined a
Pydantic input model, so it now belongs in `schemas.py`, not an ORM models module.
The old MongoDB `user_service.py` is now `user_client.py` to distinguish the HTTP
client from the actual user service.

## Install And Configure

Requires Python 3.13+ and uv. Run all commands from the repository root:

```powershell
uv sync --extra dev
Copy-Item .env.example .env
```

Edit `.env` with your local configuration. The launch commands below explicitly
load this file; importing Python modules alone does not load it.

| Variable | Default | Purpose |
| --- | --- | --- |
| `POSTGRES_DATABASE_URL` | `postgresql://postgres:postgres@localhost:5432/users` | User database |
| `MONGODB_URI` | Atlas URI in `backend/message_service/config.py` | Message database server |
| `MONGODB_DATABASE` | `chat_db` | MongoDB database name |
| `USER_SERVICE_URL` | `http://127.0.0.1:8000` | User API base URL, without `/users` |
| `CLOUDINARY_URL` | Empty | Optional image-upload credentials |

The source defaults to the original Atlas connection when `MONGODB_URI` is unset.
The `.env.example` template uses the same Atlas connection.

Create the PostgreSQL database first. For a fresh local database, create the
existing tables explicitly (startup does not create or migrate SQL tables):

```powershell
uv run --env-file .env python -c "from backend.user_service.database import create_table; create_table()"
```

This is not a schema migration tool. Existing databases need no schema/data
migration for this reorganization. Use migrations before changing table models.
MongoDB conversation indexes are created when the message service starts.

## Run

Terminal 1, user API:

```powershell
uv run --env-file .env uvicorn backend.user_service.main:app --host 127.0.0.1 --port 8000
```

Terminal 2, messages and Socket.IO:

```powershell
uv run --env-file .env uvicorn backend.message_service.main:app --host 127.0.0.1 --port 8001
```

PostgreSQL and MongoDB must be available. User docs: http://127.0.0.1:8000/docs.
Message docs: http://127.0.0.1:8001/docs. Message startup requires reachable MongoDB.

## Backend Integration

Keep the existing two-service topology: user data is owned by `user_service`,
message data by `message_service`. Messages reference positive PostgreSQL user
IDs; no user records are copied to MongoDB. `user_client.py` is the only adapter
that calls the user API. Set `USER_SERVICE_URL` to the backend's user API when
integrating with another host; it must expose the same contracts below.

| Method | Path | Contract |
| --- | --- | --- |
| GET | `/users/` | All users |
| GET | `/users/{id}` | User by integer ID; 404 when absent |
| GET | `/users/email/{email}` | User by email; 404 when absent |
| POST | `/users` | Create user; existing 200 response retained |
| PUT | `/users/{id}` or `/users/email/{email}` | Update user |
| DELETE | `/users/{id}` or `/users/email/{email}` | Delete user; 204 |
| GET | `/api/messages/contacts` | Users except the identified user |
| GET | `/api/messages/chats` | Unique conversation partners |
| GET | `/api/messages/{id}` | Conversation history, oldest first |
| POST | `/api/messages/send/{id}` | Persist message; 201 |

User create/update bodies contain `name`, `email`, `phone_number`, `password`.
User responses include those fields plus integer `id`; message contacts exclude
passwords. Message bodies accept `text`, `image`, or both. Text is trimmed and
limited to 2,000 characters; image input is limited to 8,000,000 characters.
Empty messages return 422, self-messaging 400, and missing receivers 404.
Messages retain `_id`, `senderId`, `receiverId`, `text`, `image`, `createdAt`,
and `updatedAt`. Database/user-service outages return 503; image upload failures
return 502. Realtime failures do not roll back saved messages.

Example:

```powershell
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8001/api/messages/send/2" -Headers @{"X-User-ID" = "1"} -ContentType "application/json" -Body '{"text":"Hello"}'
```

Socket.IO clients connect to port 8001 with `auth: {user_id: 1}` and listen for
`newMessage`, which contains the same message object returned by the send API.
The server checks user existence at connection time and joins that user's room.

If embedding the user routers into an existing FastAPI app:

```python
from backend.user_service.routers import router as user_router

app.include_router(user_router)
```

For messages, use `backend.message_service.main:app` as the ASGI entry point.
Importing its router alone does not install MongoDB lifespan handling, the
Socket.IO transport, rate limiting or exception handlers. Combining both into
a single ASGI process needs explicit lifespan/Socket.IO composition; this refactor
does not silently change deployment topology.

## Tests

```powershell
uv run --extra dev pytest
```

Message tests mock MongoDB, the user API, Cloudinary and realtime delivery. User
read/update/delete tests use in-memory SQLite; the create-route test mocks the
service call. These check package integration and existing HTTP behavior, not
live PostgreSQL ID generation or live database connectivity. Both service
packages are imported in the same test process to catch naming collisions.

## Security And Deployment

- `X-User-ID` and Socket.IO `user_id` identify users but do not authenticate
	them. Anyone can impersonate another user. Keep this mode local/internal.
- Existing user password storage and response fields are unchanged. Add password
	hashing, safe response schemas and authentication before public deployment.
- The original MongoDB credential is hard-coded in configuration for development.
	Do not share it publicly. Use environment variables and rotate the credential
	before deployment; it also exists in Git history.
- Keep `.env` out of Git. Configure secrets using the deployment environment.
- HTTP requests are limited to 120/minute per client IP. Use a reverse proxy for
	body-size limits and configure trusted proxy headers appropriately.
- Socket.IO defaults to same-origin access. Configure an explicit allowed-origin
	list before serving a frontend from another origin.
- Run the message service with one worker: rooms and limits are process-local.
	Add a shared Socket.IO broker and rate-limit storage before scaling workers.