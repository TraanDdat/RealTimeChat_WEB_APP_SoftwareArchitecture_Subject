from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, model_validator


class MessageCreate(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    text: Optional[str] = Field(default=None, max_length=2000)
    image: Optional[str] = Field(default=None, max_length=8_000_000)

    @model_validator(mode="after")
    def require_content(self):
        if not self.text and not self.image:
            raise ValueError("Text or image is required.")
        return self