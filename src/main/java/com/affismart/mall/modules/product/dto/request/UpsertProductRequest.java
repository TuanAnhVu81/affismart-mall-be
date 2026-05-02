package com.affismart.mall.modules.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpsertProductRequest(
		@NotNull(message = "Category ID is required")
		Long categoryId,

		@NotBlank(message = "Product name is required")
		@Size(max = 255, message = "Product name must not exceed 255 characters")
		String name,

		@NotBlank(message = "Product SKU is required")
		@Size(max = 100, message = "Product SKU must not exceed 100 characters")
		String sku,

		@Size(max = 300, message = "Product slug must not exceed 300 characters")
		String slug,

		@Size(max = 4000, message = "Product description must not exceed 4000 characters")
		String description,

		@NotNull(message = "Product price is required")
		@DecimalMin(value = "0.0", inclusive = false, message = "Product price must be greater than 0")
		BigDecimal price,

		@NotNull(message = "Stock quantity is required")
		@Min(value = 0, message = "Stock quantity must be greater than or equal to 0")
		Integer stockQuantity,

		@Size(max = 500, message = "Image URL must not exceed 500 characters")
		String imageUrl
) {
}
