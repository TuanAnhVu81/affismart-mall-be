from __future__ import annotations

import re
import unicodedata
from datetime import UTC, datetime

from google import genai
from sqlalchemy import text

from app.services.config import get_settings
from app.services.database import engine

# Explicitly unsafe/off-topic topics. The first layer stays intentionally small:
# it blocks obvious policy/off-scope requests, while the Gemini system prompt
# handles softer off-topic steering for normal conversation.
OFF_TOPIC_PATTERNS = (
    re.compile(r"\b(chinh tri|bau cu|quoc hoi|chinh phu|tong thong|thu tuong|dang cong san)\b"),
    re.compile(r"\b(chan doan|ke don thuoc|tu van dieu tri|benh vien)\b"),
    re.compile(r"\b(co bac|xo so|ca do)\b"),
    re.compile(r"\b(hack|crack|be khoa|lua dao|ma tuy)\b"),
)

RESTRICTED_ANSWER = (
    "Toi chi ho tro cac cau hoi lien quan toi mua sam, san pham, don hang, giao hang, "
    "thanh toan va affiliate cua AffiSmart Mall."
)

FALLBACK_ERROR_ANSWER = "Xin loi, toi chua the tra loi luc nay."

# SQL query to fetch active products that match a keyword in name, sku, or category
PRODUCT_CATALOG_QUERY = text(
    """
    SELECT p.id, p.name, p.slug, p.price, p.stock_quantity, c.name AS category_name
    FROM products p
    LEFT JOIN categories c ON c.id = p.category_id
    WHERE p.is_active = true
      AND (
        LOWER(p.name) LIKE :keyword
        OR LOWER(p.sku) LIKE :keyword
        OR LOWER(c.name) LIKE :keyword
      )
    ORDER BY p.name
    LIMIT 15
    """
)

# Fallback query when no keyword could be extracted from the user message
PRODUCT_ALL_QUERY = text(
    """
    SELECT p.id, p.name, p.slug, p.price, p.stock_quantity, c.name AS category_name
    FROM products p
    LEFT JOIN categories c ON c.id = p.category_id
    WHERE p.is_active = true
    ORDER BY p.name
    LIMIT 20
    """
)


class ChatService:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._client = None

    def chat(self, message: str, user_id: int | None, session_id: str | None) -> dict:
        normalized_message = message.strip()
        # Block only clearly off-topic messages; let Gemini handle on-topic enforcement
        if self._is_off_topic(normalized_message):
            return self._build_response(RESTRICTED_ANSWER, True)

        # Retrieve real product data from DB to prevent hallucination (RAG pattern)
        catalog_context = self._fetch_catalog_context(normalized_message)

        client = self._get_client()
        prompt = self._build_prompt(normalized_message, user_id, session_id, catalog_context)
        response = client.models.generate_content(
            model=self._settings.gemini_model,
            contents=prompt,
        )
        answer = getattr(response, "text", None) or FALLBACK_ERROR_ANSWER
        return self._build_response(answer.strip(), False)

    def _get_client(self):
        if self._client is not None:
            return self._client

        if not self._settings.gemini_api_key:
            raise RuntimeError("Gemini API key is not configured")

        self._client = genai.Client(api_key=self._settings.gemini_api_key)
        return self._client

    def _fetch_catalog_context(self, message: str) -> str:
        """
        Query the real product catalog from DB based on keywords in the message.
        Returns a formatted string to be injected into the Gemini prompt.
        """
        keyword = self._extract_search_keyword(message)
        try:
            with engine.connect() as conn:
                if keyword:
                    rows = conn.execute(PRODUCT_CATALOG_QUERY, {"keyword": f"%{keyword}%"}).mappings().all()
                else:
                    rows = conn.execute(PRODUCT_ALL_QUERY).mappings().all()

            if not rows:
                return "Hien tai khong co san pham phu hop voi yeu cau cua khach hang."

            # Format product list as plain text to inject into prompt
            lines = ["Danh sach san pham thuc te trong cua hang AffiSmart Mall:"]
            for row in rows:
                price_formatted = f"{int(row['price']):,}".replace(",", ".")
                stock_note = "het hang" if row["stock_quantity"] == 0 else f"con {row['stock_quantity']} san pham"
                lines.append(
                    f"- [{row['category_name']}] {row['name']} | "
                    f"Gia: {price_formatted} VND | {stock_note} | slug: {row['slug']}"
                )
            return "\n".join(lines)
        except Exception:
            # If DB query fails, return empty context; Gemini will be instructed to say so
            return "Khong the tai danh sach san pham luc nay."

    def _extract_search_keyword(self, message: str) -> str | None:
        """
        Extract the most likely product-related keyword from the user's message
        by stripping common Vietnamese stopwords and short filler words.
        Returns None to trigger the full catalog fallback when the user asks a general listing question.
        """
        normalized = self._normalize_for_topic_match(message)

        # Detect "show all products" intent — return None to use full catalog query
        general_listing_signals = {
            "tat ca", "danh sach", "cac san pham", "list all", "show all",
            "het san pham", "trong shop", "trong cua hang", "cua hang co gi",
        }
        if any(signal in normalized for signal in general_listing_signals):
            return None

        stopwords = {
            # Vietnamese filler / request words
            "cho", "toi", "xem", "muon", "tim", "mua", "can", "co", "san", "pham",
            "ban", "gi", "loai", "nao", "vay", "the", "la", "va", "de", "tu",
            "thong", "tin", "cac", "trong", "shop", "hang", "tat", "dang",
            "gia", "cua", "rat", "hay", "tot", "nhat", "biet", "cho", "biet",
            # English filler
            "show", "me", "i", "want", "to", "buy", "find", "need", "a", "an",
            "what", "which", "have", "your", "store", "any", "do", "you",
        }
        tokens = [token for token in normalized.split() if token not in stopwords and len(token) > 2]
        # Return the longest remaining token as the most meaningful keyword
        return max(tokens, key=len) if tokens else None

    def _is_off_topic(self, message: str) -> bool:
        normalized_message = self._normalize_for_topic_match(message)
        return any(pattern.search(normalized_message) for pattern in OFF_TOPIC_PATTERNS)

    def _normalize_for_topic_match(self, message: str) -> str:
        lowered_message = message.lower().replace("đ", "d")
        folded_message = "".join(
            character
            for character in unicodedata.normalize("NFKD", lowered_message)
            if not unicodedata.combining(character)
        )
        words_only = re.sub(r"[^a-z0-9]+", " ", folded_message)
        return re.sub(r"\s+", " ", words_only).strip()

    def _build_prompt(
        self,
        message: str,
        user_id: int | None,
        session_id: str | None,
        catalog_context: str,
    ) -> str:
        actor_context = f"user_id={user_id}" if user_id is not None else f"session_id={session_id or 'guest'}"
        return f"""
You are the AffiSmart Mall shopping assistant.
You MUST only answer questions using the product data provided below.
Do NOT invent, guess, or reference any products outside this list.
If the user asks for a product not in the list, say it is currently unavailable.

IMPORTANT RULES:
- Only reference products from the [PRODUCT CATALOG] section below.
- Do not make up prices, product names, or stock information.
- Answer in the same language the user is writing in (Vietnamese or English).
- Keep answers concise and helpful.

[PRODUCT CATALOG]
{catalog_context}

[CONTEXT]
{actor_context}

[USER MESSAGE]
{message}
""".strip()

    def _build_response(self, answer: str, restricted_topic: bool) -> dict:
        return {
            "answer": answer,
            "restricted_topic": restricted_topic,
            "model": self._settings.gemini_model,
            "generated_at": datetime.now(UTC),
        }
