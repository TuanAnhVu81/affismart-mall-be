package com.affismart.mall.modules.ai.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.ai.dto.request.AiChatRequest;
import com.affismart.mall.modules.ai.dto.response.AiChatResponse;
import com.affismart.mall.modules.ai.dto.response.AiRecommendationResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AiProxyService {

	private final RestClient aiRestClient;

	public AiProxyService(RestClient aiRestClient) {
		this.aiRestClient = aiRestClient;
	}

	public AiRecommendationResponse getHomepageRecommendations(Long userId, String sessionId, Integer limit) {
		try {
			return aiRestClient.get()
					.uri(uriBuilder -> {
						var builder = uriBuilder.path("/internal/recommend/homepage");
						if (userId != null) {
							builder.queryParam("user_id", userId);
						}
						if (StringUtils.hasText(sessionId)) {
							builder.queryParam("session_id", sessionId.trim());
						}
						if (limit != null) {
							builder.queryParam("limit", limit);
						}
						return builder.build();
					})
					.retrieve()
					.body(AiRecommendationResponse.class);
		} catch (RestClientException exception) {
			throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Failed to retrieve homepage recommendations");
		}
	}

	public AiRecommendationResponse getRelatedRecommendations(Long productId, Integer limit) {
		try {
			return aiRestClient.get()
					.uri(uriBuilder -> {
						var builder = uriBuilder.path("/internal/recommend/related/{productId}");
						if (limit != null) {
							builder.queryParam("limit", limit);
						}
						return builder.build(productId);
					})
					.retrieve()
					.body(AiRecommendationResponse.class);
		} catch (RestClientException exception) {
			throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Failed to retrieve related recommendations");
		}
	}

	public AiChatResponse chat(Long userId, AiChatRequest request) {
		try {
			return aiRestClient.post()
					.uri("/internal/chat")
					.body(new InternalAiChatRequest(
							userId,
							StringUtils.hasText(request.sessionId()) ? request.sessionId().trim() : null,
							request.message().trim()
					))
					.retrieve()
					.body(AiChatResponse.class);
		} catch (RestClientException exception) {
			throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Failed to retrieve AI chat response");
		}
	}

	private record InternalAiChatRequest(
			Long userId,
			String sessionId,
			String message
	) {
	}
}
