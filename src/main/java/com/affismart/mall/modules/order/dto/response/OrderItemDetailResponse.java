package com.affismart.mall.modules.order.dto.response;

import java.math.BigDecimal;

public record OrderItemDetailResponse(
		Long productId,
		String productName,
		String productSku,
		Integer quantity,
		BigDecimal priceAtTime,
		BigDecimal lineTotal
) {
}
