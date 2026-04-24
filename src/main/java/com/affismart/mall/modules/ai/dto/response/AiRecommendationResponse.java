package com.affismart.mall.modules.ai.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record AiRecommendationResponse(
		List<Long> productIds,
		boolean fallbackUsed,
		String modelVersion,
		OffsetDateTime generatedAt
) {
}
