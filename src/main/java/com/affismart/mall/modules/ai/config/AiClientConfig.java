package com.affismart.mall.modules.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

	@Bean
	public RestClient aiRestClient(AiProperties aiProperties) {
		return RestClient.builder()
				.baseUrl(aiProperties.getBaseUrl())
				.build();
	}
}
