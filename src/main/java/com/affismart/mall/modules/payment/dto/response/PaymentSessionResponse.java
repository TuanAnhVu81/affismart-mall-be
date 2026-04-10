package com.affismart.mall.modules.payment.dto.response;

public record PaymentSessionResponse(
		Long orderId,
		String sessionId,
		String paymentUrl
) {
}
