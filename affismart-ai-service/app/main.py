from fastapi import FastAPI, Response
from fastapi.middleware.cors import CORSMiddleware

from app.api.chat import router as chat_router
from app.api.recommendations import get_recommendation_service, router as recommendation_router
from app.schemas.recommendations import HealthResponse
from app.services.config import get_settings

settings = get_settings()
app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    description="AffiSmart AI Service for internal recommendations and future chatbot flows.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(recommendation_router)
app.include_router(chat_router)


@app.get("/health", response_model=HealthResponse, tags=["health"])
def health() -> HealthResponse:
    service = get_recommendation_service()
    return HealthResponse(**service.get_health())


@app.head("/health", tags=["health"])
def health_head() -> Response:
    return Response(status_code=200)
