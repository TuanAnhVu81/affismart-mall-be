package com.affismart.mall.config;

import com.affismart.mall.integration.stripe.StripeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class PaymentConfig {
}
