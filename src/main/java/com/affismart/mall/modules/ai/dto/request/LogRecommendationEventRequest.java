package com.affismart.mall.modules.ai.dto.request;

import com.affismart.mall.modules.ai.entity.RecommendationEventAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LogRecommendationEventRequest(
		@Size(max = 100) String sessionId,
		@NotNull Long productId,
		@NotNull RecommendationEventAction action
) {
}
