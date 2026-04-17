package com.affismart.mall.modules.affiliate.dto.request;

import com.affismart.mall.common.enums.PayoutRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePayoutRequestStatusRequest(
		@NotNull(message = "Status is required")
		PayoutRequestStatus status,

		@Size(max = 1000, message = "Admin note must not exceed 1000 characters")
		String adminNote
) {
}
