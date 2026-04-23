package com.affismart.mall.modules.ai.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.ai.dto.request.LogRecommendationEventRequest;
import com.affismart.mall.modules.ai.entity.RecommendationEvent;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.product.repository.ProductRepository;
import com.affismart.mall.modules.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EventLoggingService {

	private final ProductRepository productRepository;
	private final RecommendationEventWriter recommendationEventWriter;

	public EventLoggingService(
			ProductRepository productRepository,
			RecommendationEventWriter recommendationEventWriter
	) {
		this.productRepository = productRepository;
		this.recommendationEventWriter = recommendationEventWriter;
	}

	public void submitEvent(Long authenticatedUserId, LogRecommendationEventRequest request) {
		String normalizedSessionId = normalizeSessionId(request.sessionId());
		if (authenticatedUserId == null && !StringUtils.hasText(normalizedSessionId)) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Either authenticated user or session_id is required");
		}
		if (!productRepository.existsById(request.productId())) {
			throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
		}

		recommendationEventWriter.writeAsync(buildRecommendationEvent(authenticatedUserId, normalizedSessionId, request));
	}

	private RecommendationEvent buildRecommendationEvent(
			Long authenticatedUserId,
			String normalizedSessionId,
			LogRecommendationEventRequest request
	) {
		RecommendationEvent recommendationEvent = new RecommendationEvent();
		recommendationEvent.setAction(request.action());
		recommendationEvent.setSessionId(normalizedSessionId);
		recommendationEvent.setProduct(createProductReference(request.productId()));
		recommendationEvent.setUser(createUserReference(authenticatedUserId));
		return recommendationEvent;
	}

	private Product createProductReference(Long productId) {
		Product product = new Product();
		product.setId(productId);
		return product;
	}

	private User createUserReference(Long userId) {
		if (userId == null) {
			return null;
		}
		User user = new User();
		user.setId(userId);
		return user;
	}

	private String normalizeSessionId(String sessionId) {
		return StringUtils.hasText(sessionId) ? sessionId.trim() : null;
	}
}
