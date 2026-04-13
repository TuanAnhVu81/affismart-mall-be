package com.affismart.mall.modules.affiliate.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.affiliate.click-tracking")
public class AffiliateClickTrackingProperties {

	private int maxClicksPerWindow = 5;

	private Duration window = Duration.ofMinutes(1);

	private Duration blockedIpTtl = Duration.ofMinutes(5);

	private String blockReason = "RATE_LIMIT_EXCEEDED";
}
