package com.affismart.mall.modules.order.service;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.entity.OrderItem;
import com.affismart.mall.modules.order.repository.CommissionMaintenanceRepository;
import com.affismart.mall.modules.order.repository.OrderItemRepository;
import com.affismart.mall.modules.order.repository.OrderRepository;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.product.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStatusService Unit Tests")
class OrderStatusServiceTest {

	@Mock
	private OrderService orderService;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private OrderPaymentGateway orderPaymentGateway;

	@Mock
	private CommissionMaintenanceRepository commissionMaintenanceRepository;

	@InjectMocks
	private OrderStatusService orderStatusService;

	@Captor
	private ArgumentCaptor<Order> orderCaptor;

	@Test
	@DisplayName("cancelMyOrder: PENDING order is cancelled and stock is restored")
	void cancelMyOrder_PendingStatus_RestoresStockAndCancels() {
		// Given
		Long userId = 10L;
		Long orderId = 100L;
		Order order = createOrder(orderId, OrderStatus.PENDING, null);
		Product lightweightProductRef = new Product();
		lightweightProductRef.setId(1L);
		OrderItem item = createOrderItem(order, lightweightProductRef, 2);

		Product lockedProduct = new Product();
		lockedProduct.setId(1L);
		lockedProduct.setStockQuantity(5);

		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);
		given(orderItemRepository.findByOrder_Id(orderId)).willReturn(List.of(item));
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of(lockedProduct));

		// When
		orderStatusService.cancelMyOrder(userId, orderId);

		// Then
		assertThat(lockedProduct.getStockQuantity()).isEqualTo(7);
		verify(orderPaymentGateway, never()).refundForOrder(order);
		verify(commissionMaintenanceRepository, never()).rejectPendingCommissionByOrderId(orderId);
		verify(orderRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	@DisplayName("cancelMyOrder: PAID order triggers refund, rejects pending commissions, restores stock")
	void cancelMyOrder_PaidStatus_RefundsRejectsCommissionAndCancels() {
		// Given
		Long userId = 10L;
		Long orderId = 101L;
		Order order = createOrder(orderId, OrderStatus.PAID, "cs_test_123");
		Product lightweightProductRef = new Product();
		lightweightProductRef.setId(2L);
		OrderItem item = createOrderItem(order, lightweightProductRef, 1);

		Product lockedProduct = new Product();
		lockedProduct.setId(2L);
		lockedProduct.setStockQuantity(8);

		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);
		given(orderItemRepository.findByOrder_Id(orderId)).willReturn(List.of(item));
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of(lockedProduct));

		// When
		orderStatusService.cancelMyOrder(userId, orderId);

		// Then
		assertThat(lockedProduct.getStockQuantity()).isEqualTo(9);
		verify(orderPaymentGateway).refundForOrder(order);
		verify(commissionMaintenanceRepository).rejectPendingCommissionByOrderId(orderId);
		verify(orderRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	@DisplayName("cancelMyOrder: non-cancellable status throws ORDER_CANCELLATION_NOT_ALLOWED")
	void cancelMyOrder_NonCancellableStatus_ThrowsConflict() {
		// Given
		Long userId = 10L;
		Long orderId = 103L;
		Order order = createOrder(orderId, OrderStatus.SHIPPED, "cs_test_789");

		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);

		// When + Then
		assertThatThrownBy(() -> orderStatusService.cancelMyOrder(userId, orderId))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ORDER_CANCELLATION_NOT_ALLOWED);

		verify(orderItemRepository, never()).findByOrder_Id(eq(orderId));
		verify(orderRepository, never()).save(order);
	}

	@Test
	@DisplayName("cancelMyOrder: Exception - product not found while restocking throws PRODUCT_NOT_FOUND")
	void cancelMyOrder_ProductNotFoundWhileRestocking_ThrowsException() {
		// Given
		Long userId = 10L;
		Long orderId = 105L;
		Order order = createOrder(orderId, OrderStatus.PENDING, null);
		
		Product missingProductRef = new Product();
		missingProductRef.setId(99L);
		OrderItem item = createOrderItem(order, missingProductRef, 3);

		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);
		given(orderItemRepository.findByOrder_Id(orderId)).willReturn(List.of(item));
		// The database simulation fails to find the product (returns an empty list).
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of());

		// When + Then
		assertThatThrownBy(() -> orderStatusService.cancelMyOrder(userId, orderId))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

		verify(orderPaymentGateway, never()).refundForOrder(any());
		verify(orderRepository, never()).save(any());
	}

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

	private Order createOrder(Long id, OrderStatus status, String stripeSessionId) {
		Order order = new Order();
		order.setId(id);
		order.setStatus(status);
		order.setStripeSessionId(stripeSessionId);
		return order;
	}

	private OrderItem createOrderItem(Order order, Product product, int quantity) {
		OrderItem item = new OrderItem();
		item.setOrder(order);
		item.setProduct(product);
		item.setQuantity(quantity);
		return item;
	}
}
