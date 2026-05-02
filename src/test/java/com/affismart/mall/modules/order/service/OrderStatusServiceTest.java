package com.affismart.mall.modules.order.service;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.repository.CommissionMaintenanceRepository;
import com.affismart.mall.modules.order.repository.OrderRepository;
import java.util.Optional;
import org.mockito.InOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStatusService Unit Tests")
class OrderStatusServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderPaymentGateway orderPaymentGateway;

	@Mock
	private CommissionMaintenanceRepository commissionMaintenanceRepository;

	@Mock
	private OrderCancellationPersistenceService orderCancellationPersistenceService;

	@InjectMocks
	private OrderStatusService orderStatusService;

	@Captor
	private ArgumentCaptor<Order> orderCaptor;

	// =========================================================
	// cancelMyOrder()
	// =========================================================

	@Test
	@DisplayName("cancelMyOrder: PENDING order is cancelled and stock is restored")
	void cancelMyOrder_PendingStatus_RestoresStockAndCancels() {
		// Given
		Long userId = 10L;
		Long orderId = 100L;
		Order order = createOrder(orderId, OrderStatus.PENDING, null);
		given(orderCancellationPersistenceService.getCancellableOrder(userId, orderId)).willReturn(order);

		// When
		orderStatusService.cancelMyOrder(userId, orderId);

		// Then
		verify(orderPaymentGateway, never()).refundForOrder(order);
		verify(orderCancellationPersistenceService).cancelPendingOrder(userId, orderId);
		verify(orderCancellationPersistenceService, never()).finalizePaidOrderCancellation(userId, orderId);
	}

	@Test
	@DisplayName("cancelMyOrder: PAID order triggers refund outside persistence transaction then finalizes cancellation")
	void cancelMyOrder_PaidStatus_RefundsRejectsCommissionAndCancels() {
		// Given
		Long userId = 10L;
		Long orderId = 101L;
		Order order = createOrder(orderId, OrderStatus.PAID, "cs_test_123");
		given(orderCancellationPersistenceService.getCancellableOrder(userId, orderId)).willReturn(order);

		// When
		orderStatusService.cancelMyOrder(userId, orderId);

		// Then
		InOrder inOrder = inOrder(orderPaymentGateway, orderCancellationPersistenceService);
		inOrder.verify(orderPaymentGateway).refundForOrder(order);
		inOrder.verify(orderCancellationPersistenceService).finalizePaidOrderCancellation(userId, orderId);
		verify(orderCancellationPersistenceService, never()).cancelPendingOrder(userId, orderId);
	}

	@Test
	@DisplayName("cancelMyOrder: non-cancellable status throws ORDER_CANCELLATION_NOT_ALLOWED")
	void cancelMyOrder_NonCancellableStatus_ThrowsConflict() {
		// Given
		Long userId = 10L;
		Long orderId = 103L;
		given(orderCancellationPersistenceService.getCancellableOrder(userId, orderId))
				.willThrow(new AppException(
						ErrorCode.ORDER_CANCELLATION_NOT_ALLOWED,
						"Only PENDING or PAID orders can be cancelled"
				));

		// When + Then
		assertThatThrownBy(() -> orderStatusService.cancelMyOrder(userId, orderId))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ORDER_CANCELLATION_NOT_ALLOWED);

		verify(orderPaymentGateway, never()).refundForOrder(any());
		verify(orderCancellationPersistenceService, never()).cancelPendingOrder(any(), any());
		verify(orderCancellationPersistenceService, never()).finalizePaidOrderCancellation(any(), any());
	}

	@Test
	@DisplayName("cancelMyOrder: Exception - product not found while restocking throws PRODUCT_NOT_FOUND")
	void cancelMyOrder_ProductNotFoundWhileRestocking_ThrowsException() {
		// Given
		Long userId = 10L;
		Long orderId = 105L;
		Order order = createOrder(orderId, OrderStatus.PENDING, null);

		given(orderCancellationPersistenceService.getCancellableOrder(userId, orderId)).willReturn(order);
		willThrow(new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found while restoring stock"))
				.given(orderCancellationPersistenceService)
				.cancelPendingOrder(userId, orderId);

		// When + Then
		assertThatThrownBy(() -> orderStatusService.cancelMyOrder(userId, orderId))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

		verify(orderPaymentGateway, never()).refundForOrder(any());
		verify(orderRepository, never()).save(any());
	}

	// =========================================================
	// updateOrderStatusByAdmin()
	// =========================================================

	@Test
	@DisplayName("updateOrderStatusByAdmin: valid transition PAID -> CONFIRMED is saved")
	void updateOrderStatusByAdmin_ValidTransition_SavesOrder() {
		// Given
		Long orderId = 200L;
		Order order = createOrder(orderId, OrderStatus.PAID, "cs_test_200");
		given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

		// When
		orderStatusService.updateOrderStatusByAdmin(orderId, OrderStatus.CONFIRMED);

		// Then
		verify(orderRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		verify(commissionMaintenanceRepository, never()).approvePendingCommissionAndAddBalanceByOrderId(orderId);
	}

	@Test
	@DisplayName("updateOrderStatusByAdmin: SHIPPED -> DONE approves pending commission and adds affiliate balance")
	void updateOrderStatusByAdmin_ShippedToDone_ApprovesCommissionAndAddsBalance() {
		// Given
		Long orderId = 210L;
		Order order = createOrder(orderId, OrderStatus.SHIPPED, "cs_test_210");
		given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

		// When
		orderStatusService.updateOrderStatusByAdmin(orderId, OrderStatus.DONE);

		// Then
		verify(orderRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.DONE);
		verify(commissionMaintenanceRepository).approvePendingCommissionAndAddBalanceByOrderId(orderId);
	}

	@Test
	@DisplayName("updateOrderStatusByAdmin: invalid target status throws INVALID_INPUT")
	void updateOrderStatusByAdmin_InvalidTargetStatus_ThrowsInvalidInput() {
		// When + Then
		assertThatThrownBy(() -> orderStatusService.updateOrderStatusByAdmin(200L, OrderStatus.CANCELLED))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);

		verify(orderRepository, never()).findById(any());
		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("updateOrderStatusByAdmin: invalid transition throws ORDER_STATUS_TRANSITION_NOT_ALLOWED")
	void updateOrderStatusByAdmin_InvalidTransition_ThrowsTransitionError() {
		// Given
		Long orderId = 201L;
		Order order = createOrder(orderId, OrderStatus.PENDING, null);
		given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

		// When + Then
		assertThatThrownBy(() -> orderStatusService.updateOrderStatusByAdmin(orderId, OrderStatus.SHIPPED))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_NOT_ALLOWED);

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("updateOrderStatusByAdmin: missing order throws ORDER_NOT_FOUND")
	void updateOrderStatusByAdmin_OrderNotFound_ThrowsOrderNotFound() {
		// Given
		Long orderId = 999L;
		given(orderRepository.findById(orderId)).willReturn(Optional.empty());

		// When + Then
		assertThatThrownBy(() -> orderStatusService.updateOrderStatusByAdmin(orderId, OrderStatus.CONFIRMED))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ORDER_NOT_FOUND);

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("updateOrderStatusByAdmin: rollback DONE -> PENDING is rejected")
	void updateOrderStatusByAdmin_DoneToPending_ThrowsInvalidInput() {
		// When + Then
		assertThatThrownBy(() -> orderStatusService.updateOrderStatusByAdmin(300L, OrderStatus.PENDING))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);

		verify(orderRepository, never()).findById(any());
		verify(orderRepository, never()).save(any());
	}

	// =========================================================
	// markOrderPaidByWebhook()
	// =========================================================

	@Test
	@DisplayName("markOrderPaidByWebhook: pending order is updated to PAID and saves stripe session id")
	void markOrderPaidByWebhook_PendingOrder_UpdatesToPaid() {
		// Given
		Long orderId = 301L;
		Order order = createOrder(orderId, OrderStatus.PENDING, null);
		given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

		// When
		Order result = orderStatusService.markOrderPaidByWebhook(orderId, "cs_paid_301");

		// Then
		assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(result.getStripeSessionId()).isEqualTo("cs_paid_301");
		verify(orderRepository).save(order);
	}

	@Test
	@DisplayName("markOrderPaidByWebhook: already paid order returns early without save")
	void markOrderPaidByWebhook_AlreadyPaid_ReturnsEarly() {
		// Given
		Long orderId = 302L;
		Order order = createOrder(orderId, OrderStatus.PAID, "cs_paid_302");
		given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

		// When
		Order result = orderStatusService.markOrderPaidByWebhook(orderId, "cs_new");

		// Then
		assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(result.getStripeSessionId()).isEqualTo("cs_paid_302");
		verify(orderRepository, never()).save(any());
	}

	// =========================================================
	// Private Helper Methods
	// =========================================================

	private Order createOrder(Long id, OrderStatus status, String stripeSessionId) {
		Order order = new Order();
		order.setId(id);
		order.setStatus(status);
		order.setStripeSessionId(stripeSessionId);
		return order;
	}

}
