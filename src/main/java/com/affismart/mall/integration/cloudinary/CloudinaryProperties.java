package com.affismart.mall.integration.cloudinary;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage.cloudinary")
public class CloudinaryProperties {

	private boolean enabled;
	private String cloudName;
	private String apiKey;
	private String apiSecret;
	private String folder = "affismart/products";
}
