package com.affismart.mall.modules.auth.model;

public record RefreshSessionEntry(
		String sessionId,
		Long userId,
		String currentTokenHash,
		String ipAddress,
		String userAgent,
		String status,
		long createdAtEpochMs,
		long updatedAtEpochMs
) {

	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_REVOKED = "REVOKED";

	public static RefreshSessionEntry active(
			String sessionId,
			Long userId,
			String tokenHash,
			String ipAddress,
			String userAgent,
			long nowEpochMs
	) {
		return new RefreshSessionEntry(
				sessionId,
				userId,
				tokenHash,
				ipAddress,
				userAgent,
				STATUS_ACTIVE,
				nowEpochMs,
				nowEpochMs
		);
	}

	public RefreshSessionEntry rotate(String tokenHash, String ipAddress, String userAgent, long nowEpochMs) {
		return new RefreshSessionEntry(
				sessionId,
				userId,
				tokenHash,
				ipAddress,
				userAgent,
				STATUS_ACTIVE,
				createdAtEpochMs,
				nowEpochMs
		);
	}

	public RefreshSessionEntry revoke(long nowEpochMs) {
		return new RefreshSessionEntry(
				sessionId,
				userId,
				currentTokenHash,
				ipAddress,
				userAgent,
				STATUS_REVOKED,
				createdAtEpochMs,
				nowEpochMs
		);
	}
}
