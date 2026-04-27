package com.affismart.mall.modules.ai.config;

import java.net.http.HttpClient;
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

	@Bean
	public HttpClient aiHttpClient() {
		// Force HTTP/1.1 — uvicorn does not support HTTP/2 upgrade requests (h2c),
		// which causes the body to be silently dropped and returns 422 from FastAPI.
		return HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.build();
	}
}
