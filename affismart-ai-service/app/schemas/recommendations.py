from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class RecommendationResponse(BaseModel):
    product_ids: list[int] = Field(default_factory=list)
    fallback_used: bool
    model_version: str
    generated_at: datetime


class HealthResponse(BaseModel):
    status: str
    model_ready: bool
    generated_at: Optional[datetime] = None
    tracked_products: int = 0
    tracked_actors: int = 0
