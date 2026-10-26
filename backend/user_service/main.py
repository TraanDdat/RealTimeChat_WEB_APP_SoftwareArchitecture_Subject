from fastapi import FastAPI

from .routes import router


app = FastAPI(title="Chat User API")
app.include_router(router)