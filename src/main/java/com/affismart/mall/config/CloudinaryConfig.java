package com.affismart.mall.config;

import com.affismart.mall.integration.cloudinary.CloudinaryProperties;
import com.cloudinary.Cloudinary;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

	@Bean
	@ConditionalOnProperty(prefix = "app.storage.cloudinary", name = "enabled", havingValue = "true")
	public Cloudinary cloudinary(CloudinaryProperties properties) {
		validateRequiredProperty(properties.getCloudName(), "app.storage.cloudinary.cloud-name");
		validateRequiredProperty(properties.getApiKey(), "app.storage.cloudinary.api-key");
		validateRequiredProperty(properties.getApiSecret(), "app.storage.cloudinary.api-secret");

		Map<String, Object> config = new HashMap<>();
		config.put("cloud_name", properties.getCloudName());
		config.put("api_key", properties.getApiKey());
		config.put("api_secret", properties.getApiSecret());
		config.put("secure", true);
		return new Cloudinary(config);
	}

	private void validateRequiredProperty(String value, String propertyName) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalStateException("Missing required property: " + propertyName);
		}
	}
}
