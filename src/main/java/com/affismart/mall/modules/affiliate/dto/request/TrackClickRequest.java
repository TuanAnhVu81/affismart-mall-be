package com.affismart.mall.modules.affiliate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrackClickRequest(
		@NotBlank(message = "Referral code is required")
		@Size(max = 50, message = "Referral code must not exceed 50 characters")
		String refCode
) {
}
