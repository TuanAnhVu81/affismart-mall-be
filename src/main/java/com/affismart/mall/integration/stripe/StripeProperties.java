package com.affismart.mall.integration.stripe;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment.stripe")
public class StripeProperties {

	private boolean enabled;

	private String secretKey;

	private String webhookSecret;

	private String redirectBaseUrl;

	private String successUrl;

	private String cancelUrl;

	private String currency = "vnd";
}
