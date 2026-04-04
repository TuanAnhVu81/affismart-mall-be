package com.affismart.mall.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@NotBlank(message = "Full name is required")
		@Size(max = 100, message = "Full name must not exceed 100 characters")
		String fullName,

		@Size(max = 20, message = "Phone must not exceed 20 characters")
		@Pattern(
				regexp = "^(|[0-9+()\\-\\s]{8,20})$",
				message = "Phone number format is invalid"
		)
		String phone,

		@Size(max = 2000, message = "Default shipping address must not exceed 2000 characters")
		String defaultShippingAddress
) {
}
