package com.affismart.mall.integration.stripe;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.payment.service.PaymentWebhookVerifier;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Primary
@ConditionalOnProperty(prefix = "app.payment.stripe", name = "enabled", havingValue = "true")
public class StripeWebhookVerifier implements PaymentWebhookVerifier {

	private final String endpointSecret;

	public StripeWebhookVerifier(StripeProperties stripeProperties) {
		if (!StringUtils.hasText(stripeProperties.getWebhookSecret())) {
			throw new IllegalStateException("Missing required property: app.payment.stripe.webhook-secret");
		}
		this.endpointSecret = stripeProperties.getWebhookSecret().trim();
	}

	@Override
	public Event verify(String payload, String signatureHeader) {
		try {
			return Webhook.constructEvent(payload, signatureHeader, endpointSecret);
		} catch (SignatureVerificationException exception) {
			throw new AppException(ErrorCode.PAYMENT_WEBHOOK_SIGNATURE_INVALID, "Stripe webhook signature is invalid");
		} catch (RuntimeException exception) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Invalid Stripe webhook payload");
		}
	}
}
