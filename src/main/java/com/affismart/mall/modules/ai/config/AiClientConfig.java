package com.affismart.mall.modules.ai.config;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

	@Bean
	public RestClient aiRestClient(AiProperties aiProperties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(aiProperties.getConnectTimeout());
		requestFactory.setReadTimeout(aiProperties.getReadTimeout());

		return RestClient.builder()
				.baseUrl(aiProperties.getBaseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	@Bean
	public HttpClient aiHttpClient(AiProperties aiProperties) {
		// Force HTTP/1.1 because uvicorn does not support HTTP/2 upgrade requests (h2c),
		// which causes the body to be silently dropped and returns 422 from FastAPI.
		return HttpClient.newBuilder()
				.connectTimeout(aiProperties.getConnectTimeout())
				.version(HttpClient.Version.HTTP_1_1)
				.build();
	}
}
