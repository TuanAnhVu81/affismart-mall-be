package com.affismart.mall.modules.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
		Long id,
		String status,
		BigDecimal totalAmount,
		String shippingAddress,
		LocalDateTime createdAt
) {
}
