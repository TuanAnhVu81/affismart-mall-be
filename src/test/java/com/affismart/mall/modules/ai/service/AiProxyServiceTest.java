package com.affismart.mall.modules.ai.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.ai.config.AiProperties;
import com.affismart.mall.modules.ai.dto.request.AiChatRequest;
import com.affismart.mall.modules.ai.dto.response.AiChatResponse;
import com.affismart.mall.modules.ai.dto.response.AiRecommendationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiProxyService Unit Tests")
@SuppressWarnings({"rawtypes", "unchecked"})
class AiProxyServiceTest {

	@Mock
	private RestClient aiRestClient;

	@Mock
	private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

	@Mock
	private RestClient.RequestHeadersSpec requestHeadersSpec;

	@Mock
	private RestClient.ResponseSpec responseSpec;

	@Mock
	private RestClient.RequestBodyUriSpec requestBodyUriSpec;

	@Mock(answer = Answers.RETURNS_SELF)
	private RestClient.RequestBodySpec requestBodySpec;

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Spy
	private AiProperties aiProperties = new AiProperties();

	@Mock
	private HttpClient aiHttpClient;

	@Mock
	private HttpResponse<String> httpResponse;

	@InjectMocks
	private AiProxyService aiProxyService;

	@Captor
	private ArgumentCaptor<HttpRequest> httpRequestCaptor;

	@Test
	@DisplayName("getHomepageRecommendations: Happy Path - delegates to FastAPI and returns payload")
	void getHomepageRecommendations_ValidInputs_ReturnsRecommendationPayload() {
		// Given
		AiRecommendationResponse expectedResponse = createRecommendationResponse();
		given(aiRestClient.get()).willReturn(requestHeadersUriSpec);
		given(requestHeadersUriSpec.uri(any(Function.class))).willReturn(requestHeadersSpec);
		given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
		given(responseSpec.body(AiRecommendationResponse.class)).willReturn(expectedResponse);

		// When
		AiRecommendationResponse result = aiProxyService.getHomepageRecommendations(9L, "guest-123", 6);

		// Then
		assertThat(result).isEqualTo(expectedResponse);
		verify(aiRestClient).get();
		verify(responseSpec).body(AiRecommendationResponse.class);
	}

	@Test
	@DisplayName("getHomepageRecommendations: Exception Case - upstream failure throws AI_SERVICE_UNAVAILABLE")
	void getHomepageRecommendations_UpstreamFailure_ThrowsAiServiceUnavailable() {
		// Given
		given(aiRestClient.get()).willReturn(requestHeadersUriSpec);
		given(requestHeadersUriSpec.uri(any(Function.class))).willThrow(new RestClientException("AI down"));

		// When / Then
		assertThatThrownBy(() -> aiProxyService.getHomepageRecommendations(null, null, null))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
	}

	@Test
	@DisplayName("getRelatedRecommendations: Exception Case - upstream failure throws AI_SERVICE_UNAVAILABLE")
	void getRelatedRecommendations_UpstreamFailure_ThrowsAiServiceUnavailable() {
		// Given
		given(aiRestClient.get()).willReturn(requestHeadersUriSpec);
		given(requestHeadersUriSpec.uri(any(Function.class))).willThrow(new RestClientException("AI down"));

		// When / Then
		assertThatThrownBy(() -> aiProxyService.getRelatedRecommendations(55L, 4))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
	}

	@Test
	@DisplayName("chat: Happy Path - forwards user context and trimmed message to FastAPI")
	void chat_ValidRequest_ForwardsUserContextAndReturnsResponse() throws Exception {
		// Given
		AiChatRequest request = new AiChatRequest("  I want to ask about shipping fees  ", "  session-77  ");
		given(httpResponse.statusCode()).willReturn(200);
		given(httpResponse.body()).willReturn("""
				{
				  "answer": "AffiSmart ships within 2-5 business days.",
				  "restricted_topic": false,
				  "model": "gemini-1.5-flash",
				  "generated_at": "2026-04-25T08:50:46.688948Z"
				}
				""");
		given(aiHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(httpResponse);

		// When
		AiChatResponse result = aiProxyService.chat(12L, request);

		// Then
		assertThat(result.answer()).isEqualTo("AffiSmart ships within 2-5 business days.");
		verify(aiHttpClient).send(httpRequestCaptor.capture(), any(HttpResponse.BodyHandler.class));
		assertThat(httpRequestCaptor.getValue().method()).isEqualTo("POST");
		assertThat(httpRequestCaptor.getValue().uri().toString()).isEqualTo("http://localhost:8000/internal/chat");
		assertThat(httpRequestCaptor.getValue().headers().firstValue("Content-Type")).contains("application/json");
	}

	@Test
	@DisplayName("chat: HTTP Contract - sends JSON body expected by FastAPI")
	void chat_HttpClientRequest_SendsJsonBodyToFastApi() throws IOException {
		// Given
		AtomicReference<String> capturedBody = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/internal/chat", exchange -> {
			capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] response = """
					{
					  "answer": "Hello from AI",
					  "restricted_topic": false,
					  "model": "gemini-test",
					  "generated_at": "2026-04-25T08:50:46.688948Z"
					}
					""".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		});
		server.start();

		AiProperties properties = new AiProperties();
		properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
		AiProxyService service = new AiProxyService(
				RestClient.builder().baseUrl(properties.getBaseUrl()).build(),
				new ObjectMapper().findAndRegisterModules(),
				properties,
				HttpClient.newHttpClient()
		);

		// When
		try {
			AiChatResponse result = service.chat(12L, new AiChatRequest(" Hello ", " debug-session "));

			// Then
			assertThat(result.answer()).isEqualTo("Hello from AI");
			assertThat(capturedBody.get()).isEqualTo("{\"message\":\"Hello\",\"user_id\":12,\"session_id\":\"debug-session\"}");
		} finally {
			server.stop(0);
		}
	}

	@Test
	@DisplayName("chat: Exception Case - upstream failure throws AI_SERVICE_UNAVAILABLE")
	void chat_UpstreamFailure_ThrowsAiServiceUnavailable() {
		// Given
		AiChatRequest request = new AiChatRequest("shipping fee question", null);
		given(httpResponse.statusCode()).willReturn(422);
		given(httpResponse.body()).willReturn("{\"detail\":\"body missing\"}");
		try {
			given(aiHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(httpResponse);
		} catch (Exception exception) {
			throw new AssertionError(exception);
		}

		// When / Then
		assertThatThrownBy(() -> aiProxyService.chat(null, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
	}

	private AiRecommendationResponse createRecommendationResponse() {
		return new AiRecommendationResponse(
				List.of(3L, 5L, 8L),
				false,
				"20260423091500",
				OffsetDateTime.now()
		);
	}
}
