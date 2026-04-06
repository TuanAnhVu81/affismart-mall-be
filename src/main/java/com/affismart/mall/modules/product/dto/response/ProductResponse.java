package com.affismart.mall.modules.product.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
		Long id,
		Long categoryId,
		String categoryName,
		String name,
		String sku,
		String slug,
		String description,
		BigDecimal price,
		Integer stockQuantity,
		String imageUrl,
		boolean active,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
