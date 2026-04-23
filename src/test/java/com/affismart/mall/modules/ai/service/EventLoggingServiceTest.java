package com.affismart.mall.modules.ai.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.ai.dto.request.LogRecommendationEventRequest;
import com.affismart.mall.modules.ai.entity.RecommendationEvent;
import com.affismart.mall.modules.ai.entity.RecommendationEventAction;
import com.affismart.mall.modules.product.repository.ProductRepository;
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
@DisplayName("EventLoggingService Unit Tests")
class EventLoggingServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private RecommendationEventWriter recommendationEventWriter;

	@InjectMocks
	private EventLoggingService eventLoggingService;

	@Captor
	private ArgumentCaptor<RecommendationEvent> recommendationEventCaptor;

	@Test
	@DisplayName("submitEvent: Happy Path - guest event with session id delegates to async writer")
	void submitEvent_GuestWithSessionId_DelegatesToAsyncWriter() {
		// Given
		LogRecommendationEventRequest request = new LogRecommendationEventRequest(
				"  guest-session-123  ",
				8L,
				RecommendationEventAction.VIEW
		);
		given(productRepository.existsById(8L)).willReturn(true);

		// When
		eventLoggingService.submitEvent(null, request);

		// Then
		verify(recommendationEventWriter).writeAsync(recommendationEventCaptor.capture());
		RecommendationEvent savedEvent = recommendationEventCaptor.getValue();
		assertThat(savedEvent.getUser()).isNull();
		assertThat(savedEvent.getSessionId()).isEqualTo("guest-session-123");
		assertThat(savedEvent.getProduct().getId()).isEqualTo(8L);
		assertThat(savedEvent.getAction()).isEqualTo(RecommendationEventAction.VIEW);
	}

	@Test
	@DisplayName("submitEvent: Happy Path - authenticated user event does not require session id")
	void submitEvent_AuthenticatedUserWithoutSessionId_DelegatesToAsyncWriter() {
		// Given
		LogRecommendationEventRequest request = new LogRecommendationEventRequest(
				null,
				15L,
				RecommendationEventAction.ADD_TO_CART
		);
		given(productRepository.existsById(15L)).willReturn(true);

		// When
		eventLoggingService.submitEvent(21L, request);

		// Then
		verify(recommendationEventWriter).writeAsync(recommendationEventCaptor.capture());
		RecommendationEvent savedEvent = recommendationEventCaptor.getValue();
		assertThat(savedEvent.getUser()).isNotNull();
		assertThat(savedEvent.getUser().getId()).isEqualTo(21L);
		assertThat(savedEvent.getSessionId()).isNull();
	}

	@Test
	@DisplayName("submitEvent: Exception Case - guest without session id throws INVALID_INPUT")
	void submitEvent_GuestWithoutSessionId_ThrowsInvalidInput() {
		// Given
		LogRecommendationEventRequest request = new LogRecommendationEventRequest(
				"   ",
				6L,
				RecommendationEventAction.PURCHASE
		);

		// When / Then
		assertThatThrownBy(() -> eventLoggingService.submitEvent(null, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);

		verifyNoInteractions(productRepository, recommendationEventWriter);
	}

	@Test
	@DisplayName("submitEvent: Exception Case - missing product throws PRODUCT_NOT_FOUND")
	void submitEvent_ProductMissing_ThrowsProductNotFound() {
		// Given
		LogRecommendationEventRequest request = new LogRecommendationEventRequest(
				"guest-session",
				404L,
				RecommendationEventAction.VIEW
		);
		given(productRepository.existsById(404L)).willReturn(false);

		// When / Then
		assertThatThrownBy(() -> eventLoggingService.submitEvent(null, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

		verify(recommendationEventWriter, never()).writeAsync(any());
	}
}
