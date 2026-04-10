package com.affismart.mall.modules.payment.model;

public record CheckoutSessionResult(
		String sessionId,
		String checkoutUrl
) {
}
