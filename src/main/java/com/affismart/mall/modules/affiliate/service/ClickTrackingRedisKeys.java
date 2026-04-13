package com.affismart.mall.modules.affiliate.service;

public final class ClickTrackingRedisKeys {

	private static final String RATE_LIMIT_KEY_PREFIX = "affiliate:click:counter:";
	private static final String BLOCKED_IP_KEY_PREFIX = "affiliate:blocked-ip:";

	private ClickTrackingRedisKeys() {
	}

	public static String rateLimitKey(String ipAddress) {
		return RATE_LIMIT_KEY_PREFIX + ipAddress;
	}

	public static String blockedIpKey(String ipAddress) {
		return BLOCKED_IP_KEY_PREFIX + ipAddress;
	}
}
