package com.affismart.mall.modules.auth.model;

import java.util.UUID;

public record RefreshTokenSession(
		String token,
		Long userId
) {

	public static RefreshTokenSession issue(Long userId) {
		return new RefreshTokenSession(UUID.randomUUID().toString(), userId);
	}
}
