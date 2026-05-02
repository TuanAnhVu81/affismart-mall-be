package com.affismart.mall.modules.ai.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.ai.config.AiProperties;
import com.affismart.mall.modules.ai.dto.request.AiChatRequest;
import com.affismart.mall.modules.ai.dto.response.AiChatResponse;
import com.affismart.mall.modules.ai.dto.response.AiRecommendationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class AiProxyService {

	private static final Logger log = LoggerFactory.getLogger(AiProxyService.class);

	private final RestClient aiRestClient;
	private final ObjectMapper objectMapper;
	private final AiProperties aiProperties;
	private final HttpClient aiHttpClient;

	public AiProxyService(
			RestClient aiRestClient,
			ObjectMapper objectMapper,
			AiProperties aiProperties,
			HttpClient aiHttpClient
	) {
		this.aiRestClient = aiRestClient;
		this.objectMapper = objectMapper;
		this.aiProperties = aiProperties;
		this.aiHttpClient = aiHttpClient;
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
		} catch (RestClientResponseException exception) {
			log.warn(
					"AI service rejected homepage recommendations request: status={}, body={}",
					exception.getStatusCode(),
					exception.getResponseBodyAsString()
			);
			throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Failed to retrieve homepage recommendations");
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
		} catch (RestClientResponseException exception) {
			log.warn(
					"AI service rejected related recommendations request: status={}, body={}",
					exception.getStatusCode(),
					exception.getResponseBodyAsString()
			);
			throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Failed to retrieve related recommendations");
		} catch (RestClientException exception) {
			throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Failed to retrieve related recommendations");
		}
	}

	public AiChatResponse chat(Long userId, AiChatRequest request) {
		try {
			String payload = buildInternalChatPayloadJson(userId, request);
			HttpRequest httpRequest = HttpRequest.newBuilder(resolveInternalUri("/internal/chat"))
					.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.header("Accept", MediaType.APPLICATION_JSON_VALUE)
					.timeout(aiProperties.getReadTimeout())
					.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
					.build();

			HttpResponse<String> response = aiHttpClient.send(
					httpRequest,
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
			);
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				log.debug("AI service rejected chat request: status={}, body={}", response.statusCode(), response.body());
				throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Failed to retrieve AI chat response");
			}
			return objectMapper.readValue(response.body(), AiChatResponse.class);
		} catch (JsonProcessingException exception) {
			throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Failed to prepare AI chat request");
		} catch (IOException exception) {
			throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Failed to retrieve AI chat response");
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Failed to retrieve AI chat response");
		}
	}

	private URI resolveInternalUri(String path) {
		String baseUrl = aiProperties.getBaseUrl();
		String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
		String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
		return URI.create(normalizedBaseUrl).resolve(normalizedPath);
	}

	private String buildInternalChatPayloadJson(Long userId, AiChatRequest request) throws JsonProcessingException {
		return objectMapper.writeValueAsString(buildInternalChatPayload(userId, request));
	}

	private Map<String, Object> buildInternalChatPayload(Long userId, AiChatRequest request) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("message", request.message().trim());
		if (userId != null) {
			payload.put("user_id", userId);
		}
		if (StringUtils.hasText(request.sessionId())) {
			payload.put("session_id", request.sessionId().trim());
		}
		return payload;
	}
}
