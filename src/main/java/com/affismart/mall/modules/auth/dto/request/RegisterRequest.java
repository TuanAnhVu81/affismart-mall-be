package com.affismart.mall.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 255, message = "Email must not exceed 255 characters")
		String email,

		@NotBlank(message = "Password is required")
		@Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
		String password,

		@NotBlank(message = "Full name is required")
		@Size(max = 100, message = "Full name must not exceed 100 characters")
		String fullName,

		@Size(max = 20, message = "Phone must not exceed 20 characters")
		@Pattern(
				regexp = "^(|[0-9+()\\-\\s]{8,20})$",
				message = "Phone number format is invalid"
		)
		String phone
) {
}
