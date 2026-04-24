package com.affismart.mall.modules.ai.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.ai.dto.request.AiChatRequest;
import com.affismart.mall.modules.ai.dto.response.AiChatResponse;
import com.affismart.mall.modules.ai.dto.response.AiRecommendationResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

	@InjectMocks
	private AiProxyService aiProxyService;

	@Captor
	private ArgumentCaptor<Object> requestBodyCaptor;

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
	void chat_ValidRequest_ForwardsUserContextAndReturnsResponse() {
		// Given
		AiChatRequest request = new AiChatRequest("  I want to ask about shipping fees  ", "  session-77  ");
		AiChatResponse expectedResponse = createChatResponse();
		given(aiRestClient.post()).willReturn(requestBodyUriSpec);
		given(requestBodyUriSpec.uri("/internal/chat")).willReturn(requestBodySpec);
		given(requestBodySpec.retrieve()).willReturn(responseSpec);
		given(responseSpec.body(AiChatResponse.class)).willReturn(expectedResponse);

		// When
		AiChatResponse result = aiProxyService.chat(12L, request);

		// Then
		assertThat(result).isEqualTo(expectedResponse);
		verify(requestBodySpec).body(requestBodyCaptor.capture());
		assertThat(requestBodyCaptor.getValue())
				.extracting("userId", "sessionId", "message")
				.containsExactly(12L, "session-77", "I want to ask about shipping fees");
	}

	@Test
	@DisplayName("chat: Exception Case - upstream failure throws AI_SERVICE_UNAVAILABLE")
	void chat_UpstreamFailure_ThrowsAiServiceUnavailable() {
		// Given
		AiChatRequest request = new AiChatRequest("shipping fee question", null);
		given(aiRestClient.post()).willReturn(requestBodyUriSpec);
		given(requestBodyUriSpec.uri("/internal/chat")).willReturn(requestBodySpec);
		given(requestBodySpec.retrieve()).willThrow(new RestClientException("Chat service down"));

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

	private AiChatResponse createChatResponse() {
		return new AiChatResponse(
				"AffiSmart ships within 2-5 business days.",
				false,
				"gemini-1.5-flash",
				OffsetDateTime.now()
		);
	}
}
