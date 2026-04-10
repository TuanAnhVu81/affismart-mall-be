package com.affismart.mall.modules.payment.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.stripe.model.Event;
import org.springframework.stereotype.Service;

@Service
public class NoopPaymentWebhookVerifier implements PaymentWebhookVerifier {

	@Override
	public Event verify(String payload, String signatureHeader) {
		throw new AppException(
				ErrorCode.PAYMENT_WEBHOOK_CONFIG_INVALID,
				"Stripe webhook verification is not configured. Please enable app.payment.stripe and set webhook secret"
		);
	}
}
