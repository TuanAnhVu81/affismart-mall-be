package com.affismart.mall.modules.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
		@NotBlank
		@Size(max = 2000)
		String message,
		@Size(max = 100)
		String sessionId
) {
}
