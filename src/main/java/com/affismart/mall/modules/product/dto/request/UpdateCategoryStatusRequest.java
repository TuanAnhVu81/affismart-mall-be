package com.affismart.mall.modules.product.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateCategoryStatusRequest(
		@NotNull(message = "Category active status is required")
		Boolean active
) {
}
