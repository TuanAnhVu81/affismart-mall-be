package com.affismart.mall.modules.payment.service;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.repository.OrderRepository;
import com.affismart.mall.modules.order.service.CommissionService;
import com.affismart.mall.modules.order.service.OrderStatusService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentWebhookService Unit Tests")
class PaymentWebhookServiceTest {

	@Mock
	private PaymentWebhookVerifier webhookVerifier;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderStatusService orderStatusService;

	@Mock
	private CommissionService commissionService;

	@InjectMocks
	private PaymentWebhookService paymentWebhookService;

	@Test
	@DisplayName("verifyWebhook: valid payload and signature delegates to verifier")
	void verifyWebhook_ValidPayloadAndSignature_DelegatesToVerifier() {
		// Given
		String payload = "{\"id\":\"evt_test_1\",\"type\":\"checkout.session.completed\"}";
		String signature = "t=1111,v1=abc";
		Event event = new Event();
		event.setId("evt_test_1");
		event.setType("checkout.session.completed");
		given(webhookVerifier.verify(payload, signature)).willReturn(event);

		// When
		Event result = paymentWebhookService.verifyWebhook(payload, signature);

		// Then
		assertThat(result.getId()).isEqualTo("evt_test_1");
		assertThat(result.getType()).isEqualTo("checkout.session.completed");
		verify(webhookVerifier).verify(payload, signature);
	}

	@Test
	@DisplayName("verifyWebhook: missing signature throws PAYMENT_WEBHOOK_SIGNATURE_INVALID")
	void verifyWebhook_MissingSignature_ThrowsInvalidSignature() {
		// When + Then
		assertThatThrownBy(() -> paymentWebhookService.verifyWebhook("{\"id\":\"evt_test_1\"}", " "))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PAYMENT_WEBHOOK_SIGNATURE_INVALID);

		verify(webhookVerifier, never()).verify(any(), any());
	}

	@Test
	@DisplayName("verifyWebhook: empty payload throws INVALID_INPUT")
	void verifyWebhook_EmptyPayload_ThrowsInvalidInput() {
		// When + Then
		assertThatThrownBy(() -> paymentWebhookService.verifyWebhook("  ", "t=1111,v1=abc"))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);

		verify(webhookVerifier, never()).verify(any(), any());
	}

	@Test
	@DisplayName("handleVerifiedEvent: checkout.session.completed with pending order marks paid and creates commission")
	void handleVerifiedEvent_CheckoutCompletedPendingOrder_MarksPaidAndCreatesCommission() {
		// Given
		Event event = mock(Event.class);
		EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
		Session session = new Session();
		session.setId("cs_test_500");
		session.setMetadata(Map.of("orderId", "500"));

		Order currentOrder = createOrder(500L, OrderStatus.PENDING, 88L);
		Order paidOrder = createOrder(500L, OrderStatus.PAID, 88L);
		paidOrder.setStripeSessionId("cs_test_500");

		given(event.getType()).willReturn("checkout.session.completed");
		given(event.getDataObjectDeserializer()).willReturn(deserializer);
		given(deserializer.getObject()).willReturn(Optional.of(session));
		given(orderRepository.findById(500L)).willReturn(Optional.of(currentOrder));
		given(orderStatusService.markOrderPaidByWebhook(500L, "cs_test_500")).willReturn(paidOrder);

		// When
		paymentWebhookService.handleVerifiedEvent(event);

		// Then
		verify(orderStatusService).markOrderPaidByWebhook(500L, "cs_test_500");
		verify(commissionService).createPendingCommissionForPaidOrder(paidOrder);
	}

	@Test
	@DisplayName("handleVerifiedEvent: checkout.session.completed falls back to unsafe deserialization when SDK cannot deserialize object")
	void handleVerifiedEvent_CheckoutCompletedWithEmptyDeserializer_UsesUnsafeDeserialization() throws Exception {
		// Given
		Event event = mock(Event.class);
		EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
		Session session = new Session();
		session.setId("cs_test_502");
		session.setMetadata(Map.of("orderId", "502"));

		Order currentOrder = createOrder(502L, OrderStatus.PENDING, null);
		Order paidOrder = createOrder(502L, OrderStatus.PAID, null);
		paidOrder.setStripeSessionId("cs_test_502");

		given(event.getId()).willReturn("evt_test_502");
		given(event.getType()).willReturn("checkout.session.completed");
		given(event.getDataObjectDeserializer()).willReturn(deserializer);
		given(deserializer.getObject()).willReturn(Optional.empty());
		given(deserializer.deserializeUnsafe()).willReturn(session);
		given(orderRepository.findById(502L)).willReturn(Optional.of(currentOrder));
		given(orderStatusService.markOrderPaidByWebhook(502L, "cs_test_502")).willReturn(paidOrder);

		// When
		paymentWebhookService.handleVerifiedEvent(event);

		// Then
		verify(orderStatusService).markOrderPaidByWebhook(502L, "cs_test_502");
		verify(commissionService).createPendingCommissionForPaidOrder(paidOrder);
	}

	@Test
	@DisplayName("handleVerifiedEvent: already paid order skips fulfillment (idempotent)")
	void handleVerifiedEvent_AlreadyPaidOrder_SkipsFulfillment() {
		// Given
		Event event = mock(Event.class);
		EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
		Session session = new Session();
		session.setId("cs_test_501");
		session.setMetadata(Map.of("orderId", "501"));

		Order currentOrder = createOrder(501L, OrderStatus.PAID, 77L);

		given(event.getType()).willReturn("checkout.session.completed");
		given(event.getDataObjectDeserializer()).willReturn(deserializer);
		given(deserializer.getObject()).willReturn(Optional.of(session));
		given(orderRepository.findById(501L)).willReturn(Optional.of(currentOrder));

		// When
		paymentWebhookService.handleVerifiedEvent(event);

		// Then
		verify(orderStatusService, never()).markOrderPaidByWebhook(any(), any());
		verifyNoInteractions(commissionService);
	}

	@Test
	@DisplayName("handleVerifiedEvent: unsupported event type is ignored")
	void handleVerifiedEvent_UnsupportedEventType_Ignored() {
		// Given
		Event event = new Event();
		event.setType("invoice.created");

		// When
		paymentWebhookService.handleVerifiedEvent(event);

		// Then
		verifyNoInteractions(orderRepository, orderStatusService, commissionService);
	}

	private Order createOrder(Long orderId, OrderStatus status, Long affiliateAccountId) {
		Order order = new Order();
		order.setId(orderId);
		order.setStatus(status);
		order.setAffiliateAccountId(affiliateAccountId);
		order.setTotalAmount(new BigDecimal("100.00"));
		return order;
	}
}
