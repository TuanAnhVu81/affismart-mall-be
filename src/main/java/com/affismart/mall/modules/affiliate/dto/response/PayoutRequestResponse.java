package com.affismart.mall.modules.affiliate.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayoutRequestResponse(
		Long id,
		Long affiliateAccountId,
		BigDecimal amount,
		String status,
		String adminNote,
		LocalDateTime resolvedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
