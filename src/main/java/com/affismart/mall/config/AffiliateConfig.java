package com.affismart.mall.config;

import com.affismart.mall.modules.affiliate.config.AffiliateClickTrackingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AffiliateClickTrackingProperties.class)
public class AffiliateConfig {
}
