package com.affismart.mall.modules.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
		@NotBlank(message = "Message is required")
		@Size(max = 2000, message = "Message must not exceed 2000 characters")
		String message,
		@Size(max = 100, message = "Session id must not exceed 100 characters")
		String sessionId
) {
}
