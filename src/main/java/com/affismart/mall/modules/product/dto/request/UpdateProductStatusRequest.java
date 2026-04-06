package com.affismart.mall.modules.product.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(
		@NotNull(message = "Product active status is required")
		Boolean active
) {
}
