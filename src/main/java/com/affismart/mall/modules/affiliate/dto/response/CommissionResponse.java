package com.affismart.mall.modules.affiliate.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CommissionResponse(
		Long id,
		Long affiliateAccountId,
		Long orderId,
		BigDecimal orderTotalAmount,
		String orderStatus,
		BigDecimal amount,
		BigDecimal rateSnapshot,
		String status,
		Long payoutRequestId,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
