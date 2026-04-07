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
