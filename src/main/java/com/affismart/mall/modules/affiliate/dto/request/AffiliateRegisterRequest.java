package com.affismart.mall.modules.affiliate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AffiliateRegisterRequest(
		@NotBlank(message = "Promotion channel is required")
		@Size(max = 100, message = "Promotion channel must not exceed 100 characters")
		String promotionChannel,

		@NotBlank(message = "Bank info is required")
		@Size(max = 1000, message = "Bank info must not exceed 1000 characters")
		String bankInfo
) {
}
