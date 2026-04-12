package com.affismart.mall.modules.affiliate.dto.request;

import jakarta.validation.constraints.Positive;

public record CreateReferralLinkRequest(
		@Positive(message = "Product id must be greater than zero")
		Long productId
) {
}
