package com.affismart.mall.modules.ai.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiClientConfig Unit Tests")
class AiClientConfigTest {

	@Test
	@DisplayName("aiHttpClient: applies connect timeout and keeps HTTP/1.1 for uvicorn")
	void aiHttpClient_AppliesTimeoutAndHttpVersion() {
		// Given
		AiProperties properties = new AiProperties();
		properties.setConnectTimeout(Duration.ofSeconds(3));
		AiClientConfig config = new AiClientConfig();

		// When
		HttpClient client = config.aiHttpClient(properties);

		// Then
		assertThat(client.connectTimeout()).contains(Duration.ofSeconds(3));
		assertThat(client.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
	}

	@Test
	@DisplayName("aiRestClient: builds successfully with configured request timeouts")
	void aiRestClient_ConfiguredTimeouts_BuildsClient() {
		// Given
		AiProperties properties = new AiProperties();
		properties.setBaseUrl("http://localhost:8000");
		properties.setConnectTimeout(Duration.ofSeconds(3));
		properties.setReadTimeout(Duration.ofSeconds(10));
		AiClientConfig config = new AiClientConfig();

		// When
		RestClient client = config.aiRestClient(properties);

		// Then
		assertThat(client).isNotNull();
	}
}
