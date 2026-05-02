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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCancellationPersistenceService Unit Tests")
class OrderCancellationPersistenceServiceTest {

	@Mock
	private OrderService orderService;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CommissionMaintenanceRepository commissionMaintenanceRepository;

	@InjectMocks
	private OrderCancellationPersistenceService service;

	@Captor
	private ArgumentCaptor<Order> orderCaptor;

	@Test
	@DisplayName("cancelPendingOrder: restores stock and marks order cancelled")
	void cancelPendingOrder_RestoresStockAndCancels() {
		// Given
		Long userId = 10L;
		Long orderId = 100L;
		Order order = createOrder(orderId, OrderStatus.PENDING);
		OrderItem item = createOrderItem(order, 1L, 2);
		Product lockedProduct = createProduct(1L, 5);

		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);
		given(orderItemRepository.findByOrder_Id(orderId)).willReturn(List.of(item));
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of(lockedProduct));

		// When
		service.cancelPendingOrder(userId, orderId);

		// Then
		assertThat(lockedProduct.getStockQuantity()).isEqualTo(7);
		verify(commissionMaintenanceRepository, never()).rejectPendingCommissionByOrderId(orderId);
		verify(orderRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	@DisplayName("finalizePaidOrderCancellation: restores stock, rejects commission, and marks order cancelled")
	void finalizePaidOrderCancellation_RestoresStockRejectsCommissionAndCancels() {
		// Given
		Long userId = 10L;
		Long orderId = 101L;
		Order order = createOrder(orderId, OrderStatus.PAID);
		OrderItem item = createOrderItem(order, 2L, 1);
		Product lockedProduct = createProduct(2L, 8);

		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);
		given(orderItemRepository.findByOrder_Id(orderId)).willReturn(List.of(item));
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of(lockedProduct));

		// When
		service.finalizePaidOrderCancellation(userId, orderId);

		// Then
		assertThat(lockedProduct.getStockQuantity()).isEqualTo(9);
		verify(commissionMaintenanceRepository).rejectPendingCommissionByOrderId(orderId);
		verify(orderRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	@DisplayName("cancelPendingOrder: missing product while restoring stock throws PRODUCT_NOT_FOUND")
	void cancelPendingOrder_ProductMissingDuringRestock_ThrowsProductNotFound() {
		// Given
		Long userId = 10L;
		Long orderId = 102L;
		Order order = createOrder(orderId, OrderStatus.PENDING);
		OrderItem item = createOrderItem(order, 99L, 3);

		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);
		given(orderItemRepository.findByOrder_Id(orderId)).willReturn(List.of(item));
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of());

		// When / Then
		assertThatThrownBy(() -> service.cancelPendingOrder(userId, orderId))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

		verify(orderRepository, never()).save(order);
	}

	private Order createOrder(Long orderId, OrderStatus status) {
		Order order = new Order();
		order.setId(orderId);
		order.setStatus(status);
		return order;
	}

	private OrderItem createOrderItem(Order order, Long productId, int quantity) {
		Product product = new Product();
		product.setId(productId);

		OrderItem item = new OrderItem();
		item.setOrder(order);
		item.setProduct(product);
		item.setQuantity(quantity);
		return item;
	}

	private Product createProduct(Long productId, int stockQuantity) {
		Product product = new Product();
		product.setId(productId);
		product.setStockQuantity(stockQuantity);
		return product;
	}
}
