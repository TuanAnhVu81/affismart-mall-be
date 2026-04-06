package com.affismart.mall.modules.auth.model;

public record RefreshTokenSession(
		String token,
		Long userId,
		String sessionId
) {

	public RefreshTokenSession(String token, Long userId) {
		this(token, userId, null);
	}
}
