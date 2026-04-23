from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from threading import Lock
from typing import Any

import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from sqlalchemy import text

from app.services.config import get_settings
from app.services.database import engine

ACTION_WEIGHTS = {
    "VIEW": 1.0,
    "ADD_TO_CART": 3.0,
    "PURCHASE": 5.0,
}

EVENTS_QUERY = text(
    """
    SELECT
        re.user_id,
        re.session_id,
        re.product_id,
        re.action,
        re.created_at
    FROM recommendation_events re
    JOIN products p ON p.id = re.product_id
    WHERE p.is_active = true
      AND re.created_at >= :cutoff_time
    ORDER BY re.created_at DESC
    """
)


@dataclass(slots=True)
class ModelSnapshot:
    generated_at: datetime
    product_ids: list[int]
    product_index: dict[int, int]
    actor_vectors: dict[str, np.ndarray]
    similarity_matrix: np.ndarray
    popularity_rank: list[int]

    @property
    def model_version(self) -> str:
        return self.generated_at.strftime("%Y%m%d%H%M%S")


class RecommendationService:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._lock = Lock()
        self._snapshot: ModelSnapshot | None = None

    def get_homepage_recommendations(
        self,
        user_id: int | None,
        session_id: str | None,
        limit: int | None,
    ) -> dict[str, Any]:
        snapshot = self._get_snapshot()
        resolved_limit = self._resolve_limit(limit)
        actor_key = self._build_actor_key(user_id, session_id)

        if snapshot is None:
            return self._empty_result()

        recommended_ids: list[int] = []
        fallback_used = True

        if actor_key and actor_key in snapshot.actor_vectors:
            recommended_ids = self._score_for_actor(snapshot, snapshot.actor_vectors[actor_key], resolved_limit)
            fallback_used = len(recommended_ids) == 0

        if not recommended_ids:
            recommended_ids = snapshot.popularity_rank[:resolved_limit]
            fallback_used = True

        return self._build_result(snapshot, recommended_ids, fallback_used)

    def get_related_recommendations(self, product_id: int, limit: int | None) -> dict[str, Any]:
        snapshot = self._get_snapshot()
        resolved_limit = self._resolve_limit(limit)

        if snapshot is None:
            return self._empty_result()

        if product_id not in snapshot.product_index:
            fallback_ids = [candidate_id for candidate_id in snapshot.popularity_rank if candidate_id != product_id]
            return self._build_result(snapshot, fallback_ids[:resolved_limit], True)

        product_idx = snapshot.product_index[product_id]
        scores = snapshot.similarity_matrix[product_idx].copy()
        scores[product_idx] = -1.0
        ranked_indices = np.argsort(scores)[::-1]

        related_ids = [
            snapshot.product_ids[idx]
            for idx in ranked_indices
            if scores[idx] > 0 and snapshot.product_ids[idx] != product_id
        ][:resolved_limit]

        if not related_ids:
            fallback_ids = [candidate_id for candidate_id in snapshot.popularity_rank if candidate_id != product_id]
            return self._build_result(snapshot, fallback_ids[:resolved_limit], True)

        return self._build_result(snapshot, related_ids, False)

    def get_health(self) -> dict[str, Any]:
        snapshot = self._get_snapshot()
        return {
            "status": "ok",
            "model_ready": snapshot is not None,
            "generated_at": snapshot.generated_at if snapshot else None,
            "tracked_products": len(snapshot.product_ids) if snapshot else 0,
            "tracked_actors": len(snapshot.actor_vectors) if snapshot else 0,
        }

    def _get_snapshot(self) -> ModelSnapshot | None:
        now = datetime.now(UTC)
        if self._snapshot and (now - self._snapshot.generated_at).total_seconds() < self._settings.model_refresh_seconds:
            return self._snapshot

        with self._lock:
            if self._snapshot and (now - self._snapshot.generated_at).total_seconds() < self._settings.model_refresh_seconds:
                return self._snapshot
            self._snapshot = self._train_snapshot(now)
            return self._snapshot

    def _train_snapshot(self, generated_at: datetime) -> ModelSnapshot | None:
        cutoff_time = datetime.now(UTC) - timedelta(days=self._settings.event_lookback_days)

        with engine.connect() as connection:
            rows = connection.execute(EVENTS_QUERY, {"cutoff_time": cutoff_time}).mappings().all()

        actor_product_scores: dict[str, dict[int, float]] = defaultdict(lambda: defaultdict(float))
        popularity_scores: dict[int, float] = defaultdict(float)
        product_ids_set: set[int] = set()

        for row in rows:
            actor_key = self._build_actor_key(row.get("user_id"), row.get("session_id"))
            if actor_key is None:
                continue

            product_id = int(row["product_id"])
            weight = ACTION_WEIGHTS.get(str(row["action"]), 1.0)
            actor_product_scores[actor_key][product_id] += weight
            popularity_scores[product_id] += weight
            product_ids_set.add(product_id)

        if not actor_product_scores or not product_ids_set:
            return None

        product_ids = sorted(product_ids_set)
        product_index = {product_id: index for index, product_id in enumerate(product_ids)}
        actor_keys = list(actor_product_scores.keys())
        interaction_matrix = np.zeros((len(actor_keys), len(product_ids)), dtype=float)

        for actor_index, actor_key in enumerate(actor_keys):
            for product_id, score in actor_product_scores[actor_key].items():
                interaction_matrix[actor_index, product_index[product_id]] = score

        similarity_matrix = cosine_similarity(interaction_matrix.T)
        actor_vectors = {
            actor_key: interaction_matrix[index]
            for index, actor_key in enumerate(actor_keys)
        }
        popularity_rank = [
            product_id
            for product_id, _ in sorted(popularity_scores.items(), key=lambda item: (-item[1], item[0]))
        ][: self._settings.popularity_fallback_limit]

        return ModelSnapshot(
            generated_at=generated_at,
            product_ids=product_ids,
            product_index=product_index,
            actor_vectors=actor_vectors,
            similarity_matrix=similarity_matrix,
            popularity_rank=popularity_rank,
        )

    def _score_for_actor(self, snapshot: ModelSnapshot, actor_vector: np.ndarray, limit: int) -> list[int]:
        interacted_indices = np.flatnonzero(actor_vector)
        if len(interacted_indices) == 0:
            return []

        scores = np.zeros(len(snapshot.product_ids), dtype=float)
        for interacted_index in interacted_indices:
            scores += snapshot.similarity_matrix[interacted_index] * actor_vector[interacted_index]

        scores[interacted_indices] = -1.0
        ranked_indices = np.argsort(scores)[::-1]

        return [snapshot.product_ids[idx] for idx in ranked_indices if scores[idx] > 0][:limit]

    def _build_actor_key(self, user_id: int | None, session_id: str | None) -> str | None:
        if user_id is not None:
            return f"user:{int(user_id)}"
        if session_id and str(session_id).strip():
            return f"session:{str(session_id).strip()}"
        return None

    def _resolve_limit(self, limit: int | None) -> int:
        resolved_limit = limit or self._settings.default_limit
        return max(1, min(resolved_limit, self._settings.max_limit))

    def _build_result(self, snapshot: ModelSnapshot, product_ids: list[int], fallback_used: bool) -> dict[str, Any]:
        return {
            "product_ids": product_ids,
            "fallback_used": fallback_used,
            "model_version": snapshot.model_version,
            "generated_at": snapshot.generated_at,
        }

    def _empty_result(self) -> dict[str, Any]:
        generated_at = datetime.now(UTC)
        return {
            "product_ids": [],
            "fallback_used": True,
            "model_version": generated_at.strftime("%Y%m%d%H%M%S"),
            "generated_at": generated_at,
        }
