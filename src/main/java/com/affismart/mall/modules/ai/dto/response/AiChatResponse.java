package com.affismart.mall.modules.ai.dto.response;

import java.time.OffsetDateTime;

public record AiChatResponse(
		String answer,
		boolean restrictedTopic,
		String model,
		OffsetDateTime generatedAt
) {
}
