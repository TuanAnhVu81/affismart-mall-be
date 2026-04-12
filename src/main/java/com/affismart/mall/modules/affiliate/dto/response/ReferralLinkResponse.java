package com.affismart.mall.modules.affiliate.dto.response;

import java.time.LocalDateTime;

public record ReferralLinkResponse(
		Long id,
		Long affiliateAccountId,
		Long productId,
		String refCode,
		Integer totalClicks,
		Integer totalConversions,
		boolean active,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
