from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    user_id: Optional[int] = Field(default=None, ge=1)
    session_id: Optional[str] = Field(default=None, max_length=100)


class ChatResponse(BaseModel):
    answer: str
    restricted_topic: bool
    model: str
    generated_at: datetime
