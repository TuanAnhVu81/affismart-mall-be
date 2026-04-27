package com.affismart.mall.modules.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record AiChatResponse(
		String answer,
		@JsonProperty("restricted_topic")
		boolean restrictedTopic,
		String model,
		@JsonProperty("generated_at")
		OffsetDateTime generatedAt
) {
}
