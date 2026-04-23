package com.affismart.mall.modules.ai.service;

import com.affismart.mall.modules.ai.entity.RecommendationEvent;
import com.affismart.mall.modules.ai.entity.RecommendationEventAction;
import com.affismart.mall.modules.ai.repository.RecommendationEventRepository;
import com.affismart.mall.modules.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationEventWriter Unit Tests")
class RecommendationEventWriterTest {

	@Mock
	private RecommendationEventRepository recommendationEventRepository;

	@InjectMocks
	private RecommendationEventWriter recommendationEventWriter;

	@Captor
	private ArgumentCaptor<RecommendationEvent> recommendationEventCaptor;

	@Test
	@DisplayName("writeAsync: Happy Path - persists recommendation event")
	void writeAsync_ValidEvent_PersistsRecommendationEvent() {
		// Given
		RecommendationEvent recommendationEvent = createRecommendationEvent(11L, RecommendationEventAction.PURCHASE);

		// When
		recommendationEventWriter.writeAsync(recommendationEvent).join();

		// Then
		verify(recommendationEventRepository).save(recommendationEventCaptor.capture());
		RecommendationEvent savedEvent = recommendationEventCaptor.getValue();
		assertThat(savedEvent.getProduct().getId()).isEqualTo(11L);
		assertThat(savedEvent.getAction()).isEqualTo(RecommendationEventAction.PURCHASE);
	}

	private RecommendationEvent createRecommendationEvent(Long productId, RecommendationEventAction action) {
		Product product = new Product();
		product.setId(productId);

		RecommendationEvent recommendationEvent = new RecommendationEvent();
		recommendationEvent.setProduct(product);
		recommendationEvent.setSessionId("session-123");
		recommendationEvent.setAction(action);
		return recommendationEvent;
	}
}
