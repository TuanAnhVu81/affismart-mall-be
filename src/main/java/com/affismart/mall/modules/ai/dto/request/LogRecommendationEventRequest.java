package com.affismart.mall.modules.ai.dto.request;

import com.affismart.mall.modules.ai.entity.RecommendationEventAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LogRecommendationEventRequest(
		@Size(max = 100, message = "Session id must not exceed 100 characters")
		String sessionId,

		@NotNull(message = "Product ID is required")
		@Positive(message = "Product ID must be greater than zero")
		Long productId,

		@NotNull(message = "Recommendation action is required")
		RecommendationEventAction action
) {
}
