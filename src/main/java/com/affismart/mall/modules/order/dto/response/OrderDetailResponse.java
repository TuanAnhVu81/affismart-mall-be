package com.affismart.mall.modules.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
		Long id,
		String status,
		BigDecimal totalAmount,
		BigDecimal discountAmount,
		String shippingAddress,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<OrderItemDetailResponse> items
) {
}
