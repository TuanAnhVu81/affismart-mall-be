package com.affismart.mall.modules.auth.model;

import com.affismart.mall.modules.auth.dto.response.AuthTokenResponse;

public record AuthenticatedSession(
		AuthTokenResponse tokenResponse,
		String refreshToken
) {
}
