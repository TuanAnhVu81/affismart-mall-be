package com.affismart.mall.modules.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentSessionRequest(
		@NotNull(message = "orderId is required")
		@Positive(message = "orderId must be greater than zero")
		Long orderId
) {
}
