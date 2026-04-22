package com.affismart.mall.modules.affiliate.dto.response;

import java.time.LocalDateTime;

public record BlockedIpResponse(
		String ipAddress,
		String reason,
		LocalDateTime blockedAt,
		LocalDateTime expiresAt
) {
}
