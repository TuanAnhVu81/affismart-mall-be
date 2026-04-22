package com.affismart.mall.modules.payment.service;

import com.affismart.mall.integration.stripe.StripeProperties;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PaymentRedirectService {

	private final StripeProperties stripeProperties;

	public PaymentRedirectService(StripeProperties stripeProperties) {
		this.stripeProperties = stripeProperties;
	}

	public String getStripeSuccessReturnUrl() {
		return UriComponentsBuilder.fromUriString(getRequiredRedirectBaseUrl())
				.path("/api/v1/payment/success")
				.queryParam("session_id", "{CHECKOUT_SESSION_ID}")
				.build(false)
				.toUriString();
	}

	public String getStripeCancelReturnUrl() {
		return UriComponentsBuilder.fromUriString(getRequiredRedirectBaseUrl())
				.path("/api/v1/payment/cancel")
				.build(false)
				.toUriString();
	}

	public ResponseEntity<Void> redirectToFrontendSuccess(String sessionId) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getRequiredFrontendSuccessUrl());
		if (StringUtils.hasText(sessionId)) {
			builder.replaceQueryParam("session_id", sessionId.trim());
		}
		return redirect(builder.build(true).toUri());
	}

	public ResponseEntity<Void> redirectToFrontendCancel() {
		return redirect(UriComponentsBuilder.fromUriString(getRequiredFrontendCancelUrl()).build(true).toUri());
	}

	private ResponseEntity<Void> redirect(URI location) {
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(location)
				.build();
	}

	private String getRequiredRedirectBaseUrl() {
		return requireText(stripeProperties.getRedirectBaseUrl(), "app.payment.stripe.redirect-base-url");
	}

	private String getRequiredFrontendSuccessUrl() {
		return requireText(stripeProperties.getSuccessUrl(), "app.payment.stripe.success-url");
	}

	private String getRequiredFrontendCancelUrl() {
		return requireText(stripeProperties.getCancelUrl(), "app.payment.stripe.cancel-url");
	}

	private String requireText(String value, String propertyName) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalStateException("Missing required property: " + propertyName);
		}
		return value.trim();
	}
}
