package com.affismart.mall.modules.affiliate.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateReferralLinkStatusRequest(
		@NotNull(message = "Active flag is required")
		Boolean active
) {
}
