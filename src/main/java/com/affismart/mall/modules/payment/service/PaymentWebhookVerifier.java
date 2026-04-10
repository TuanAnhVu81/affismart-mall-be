package com.affismart.mall.modules.payment.service;

import com.stripe.model.Event;

public interface PaymentWebhookVerifier {

	Event verify(String payload, String signatureHeader);
}
