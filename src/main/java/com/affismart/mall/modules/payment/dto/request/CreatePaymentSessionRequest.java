package com.affismart.mall.modules.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentSessionRequest(
		@NotNull(message = "orderId is required")
		Long orderId
) {
}
