package com.affismart.mall.modules.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertCategoryRequest(
		@NotBlank(message = "Category name is required")
		@Size(max = 100, message = "Category name must not exceed 100 characters")
		String name,

		@Size(max = 120, message = "Category slug must not exceed 120 characters")
		String slug
) {
}
