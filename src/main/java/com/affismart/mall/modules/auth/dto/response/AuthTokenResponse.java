package com.affismart.mall.modules.auth.dto.response;

import java.time.Instant;

public record AuthTokenResponse(
		String accessToken,
		String tokenType,
		Instant expiresAt,
		AuthUserResponse user
) {
}
