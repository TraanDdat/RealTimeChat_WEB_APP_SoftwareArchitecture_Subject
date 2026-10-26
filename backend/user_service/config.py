import os


DATABASE_URL = os.getenv(
    # "POSTGRES_DATABASE_URL", "postgresql://postgres:postgres@localhost:5432/users"
    "POSTGRES_DATABASE_URL", "postgresql://postgres:postgres@172.22.55.163:5432/users"
)