package com.affismart.mall.modules.affiliate.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPayoutRequestResponse(
		Long id,
		Long affiliateAccountId,
		Long affiliateUserId,
		String affiliateFullName,
		String affiliateEmail,
		String affiliateRefCode,
		String bankInfo,
		BigDecimal amount,
		String status,
		String adminNote,
		LocalDateTime resolvedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
