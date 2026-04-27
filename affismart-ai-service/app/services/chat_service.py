from __future__ import annotations

from datetime import UTC, datetime

import google.generativeai as genai

from app.services.config import get_settings

SHOPPING_KEYWORDS = {
    "affismart",
    "product",
    "products",
    "price",
    "pricing",
    "order",
    "orders",
    "shipping",
    "delivery",
    "refund",
    "return",
    "payment",
    "cart",
    "affiliate",
    "commission",
    "category",
    "stock",
    "khach",
    "san pham", "sản phẩm",
    "gia", "giá",
    "don hang", "đơn hàng",
    "giao hang", "giao hàng",
    "van chuyen", "vận chuyển",
    "hoan tien", "hoàn tiền",
    "thanh toan", "thanh toán",
    "gio hang", "giỏ hàng",
    "khuyen mai", "khuyến mãi",
    "cong tac vien", "cộng tác viên",
    "hello", "hi", "chao", "chào",
}

RESTRICTED_ANSWER = (
    "Toi chi ho tro cac cau hoi lien quan toi mua sam, san pham, don hang, giao hang, "
    "thanh toan va affiliate cua AffiSmart Mall."
)

FALLBACK_ERROR_ANSWER = "Xin loi, toi chua the tra loi luc nay."


class ChatService:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._model = None

    def chat(self, message: str, user_id: int | None, session_id: str | None) -> dict:
        normalized_message = message.strip()
        if not self._is_shopping_related(normalized_message):
            return self._build_response(RESTRICTED_ANSWER, True)

        model = self._get_model()
        prompt = self._build_prompt(normalized_message, user_id, session_id)
        response = model.generate_content(prompt)
        answer = getattr(response, "text", None) or FALLBACK_ERROR_ANSWER
        return self._build_response(answer.strip(), False)

    def _get_model(self):
        if self._model is not None:
            return self._model

        if not self._settings.gemini_api_key:
            raise RuntimeError("Gemini API key is not configured")

        genai.configure(api_key=self._settings.gemini_api_key)
        self._model = genai.GenerativeModel(self._settings.gemini_model)
        return self._model

    def _is_shopping_related(self, message: str) -> bool:
        lowered_message = message.lower()
        return any(keyword in lowered_message for keyword in SHOPPING_KEYWORDS)

    def _build_prompt(self, message: str, user_id: int | None, session_id: str | None) -> str:
        actor_context = f"user_id={user_id}" if user_id is not None else f"session_id={session_id or 'guest'}"
        return f"""
You are the AffiSmart Mall shopping assistant.
Only answer questions related to AffiSmart Mall products, pricing, ordering, shipping,
returns, payments, promotions, and affiliate program guidance.
If the user asks anything outside that scope, politely refuse and steer back to store-related topics.

Context: {actor_context}
User message: {message}
""".strip()

    def _build_response(self, answer: str, restricted_topic: bool) -> dict:
        return {
            "answer": answer,
            "restricted_topic": restricted_topic,
            "model": self._settings.gemini_model,
            "generated_at": datetime.now(UTC),
        }
