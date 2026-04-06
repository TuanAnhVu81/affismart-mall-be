package com.affismart.mall.modules.product.dto.response;

import java.time.LocalDateTime;

public record CategoryResponse(
		Long id,
		String name,
		String slug,
		boolean active,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
