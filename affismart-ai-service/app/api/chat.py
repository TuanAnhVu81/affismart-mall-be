from functools import lru_cache

from fastapi import APIRouter, HTTPException

from app.schemas.chat import ChatRequest, ChatResponse
from app.services.chat_service import ChatService

router = APIRouter(prefix="/internal", tags=["internal-chat"])


@lru_cache(maxsize=1)
def get_chat_service() -> ChatService:
    return ChatService()


@router.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    try:
        result = get_chat_service().chat(
            message=request.message,
            user_id=request.user_id,
            session_id=request.session_id,
        )
        return ChatResponse(**result)
    except RuntimeError as exception:
        raise HTTPException(status_code=503, detail=str(exception)) from exception
