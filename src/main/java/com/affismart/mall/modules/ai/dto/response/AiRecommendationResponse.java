package com.affismart.mall.modules.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record AiRecommendationResponse(
		@JsonProperty("product_ids")
		List<Long> productIds,
		@JsonProperty("fallback_used")
		boolean fallbackUsed,
		@JsonProperty("model_version")
		String modelVersion,
		@JsonProperty("generated_at")
		OffsetDateTime generatedAt
) {
}
