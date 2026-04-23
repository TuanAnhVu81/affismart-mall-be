from functools import lru_cache

from fastapi import APIRouter, Query

from app.schemas.recommendations import RecommendationResponse
from app.services.recommendation_service import RecommendationService

router = APIRouter(prefix="/internal/recommend", tags=["internal-recommendations"])


@lru_cache(maxsize=1)
def get_recommendation_service() -> RecommendationService:
    return RecommendationService()


@router.get("/homepage", response_model=RecommendationResponse)
def get_homepage_recommendations(
    user_id: int | None = Query(default=None, ge=1),
    session_id: str | None = Query(default=None, min_length=1, max_length=100),
    limit: int | None = Query(default=None, ge=1, le=24),
) -> RecommendationResponse:
    result = get_recommendation_service().get_homepage_recommendations(
        user_id=user_id,
        session_id=session_id,
        limit=limit,
    )
    return RecommendationResponse(**result)


@router.get("/related/{product_id}", response_model=RecommendationResponse)
def get_related_recommendations(
    product_id: int,
    limit: int | None = Query(default=None, ge=1, le=24),
) -> RecommendationResponse:
    result = get_recommendation_service().get_related_recommendations(product_id=product_id, limit=limit)
    return RecommendationResponse(**result)
