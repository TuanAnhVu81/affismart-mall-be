package com.affismart.mall.modules.affiliate.dto.request;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAffiliateAccountStatusRequest(
		@NotNull(message = "Status is required")
		AffiliateAccountStatus status
) {
}
