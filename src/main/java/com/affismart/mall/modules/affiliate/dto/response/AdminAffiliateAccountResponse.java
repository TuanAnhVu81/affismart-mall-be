package com.affismart.mall.modules.affiliate.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminAffiliateAccountResponse(
		Long id,
		Long userId,
		String fullName,
		String email,
		String bankInfo,
		String refCode,
		String promotionChannel,
		String status,
		BigDecimal commissionRate,
		BigDecimal balance,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
