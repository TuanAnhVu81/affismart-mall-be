package com.affismart.mall.modules.auth.dto.response;

import java.time.Instant;
import java.util.List;

public record AuthTokenResponse(
		Long userId,
		String email,
		List<String> roles,
		String accessToken,
		String tokenType,
		Instant expiresAt
) {
}
