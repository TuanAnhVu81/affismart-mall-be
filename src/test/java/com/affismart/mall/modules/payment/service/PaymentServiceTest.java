package com.affismart.mall.modules.payment.service;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.entity.OrderItem;
import com.affismart.mall.modules.order.repository.OrderItemRepository;
import com.affismart.mall.modules.order.repository.OrderRepository;
import com.affismart.mall.modules.order.service.OrderService;
import com.affismart.mall.modules.payment.dto.response.PaymentSessionResponse;
import com.affismart.mall.modules.payment.model.CheckoutSessionResult;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.user.entity.User;
import java.math.BigDecimal;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

	@Mock
	private OrderService orderService;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private CheckoutSessionGateway checkoutSessionGateway;

	@InjectMocks
	private PaymentService paymentService;

	@Captor
	private ArgumentCaptor<Order> orderCaptor;

	@Test
	@DisplayName("createCheckoutSession: valid pending order returns checkout url and saves stripe session id")
	void createCheckoutSession_ValidPendingOrder_ReturnsPaymentSession() {
		// Given
		Long userId = 7L;
		Long orderId = 1001L;
		Order order = createOrder(orderId, userId, OrderStatus.PENDING, null);
		OrderItem orderItem = createOrderItem(11L, "iPhone 16", new BigDecimal("29990000"), 1);
		CheckoutSessionResult sessionResult = new CheckoutSessionResult(
				"cs_test_1001",
				"https://checkout.stripe.com/c/pay/cs_test_1001"
		);

		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);
		given(checkoutSessionGateway.findReusableCheckoutSession(order)).willReturn(Optional.empty());
		given(orderItemRepository.findByOrder_IdWithProduct(orderId)).willReturn(List.of(orderItem));
		given(checkoutSessionGateway.createCheckoutSession(order, List.of(orderItem))).willReturn(sessionResult);
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

		// When
		PaymentSessionResponse response = paymentService.createCheckoutSession(userId, orderId);

		// Then
		assertThat(response.orderId()).isEqualTo(orderId);
		assertThat(response.sessionId()).isEqualTo("cs_test_1001");
		assertThat(response.paymentUrl()).isEqualTo("https://checkout.stripe.com/c/pay/cs_test_1001");

		verify(orderRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getStripeSessionId()).isEqualTo("cs_test_1001");
	}

	@Test
	@DisplayName("createCheckoutSession: non-pending order throws ORDER_PAYMENT_NOT_ALLOWED")
	void createCheckoutSession_OrderNotPending_ThrowsPaymentNotAllowed() {
		// Given
		Long userId = 7L;
		Long orderId = 1002L;
		Order order = createOrder(orderId, userId, OrderStatus.PAID, null);
		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);

		// When + Then
		assertThatThrownBy(() -> paymentService.createCheckoutSession(userId, orderId))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ORDER_PAYMENT_NOT_ALLOWED);

		verifyNoInteractions(orderItemRepository, checkoutSessionGateway, orderRepository);
	}

	@Test
	@DisplayName("createCheckoutSession: existing open stripe session returns reusable payment url")
	void createCheckoutSession_ExistingReusableSession_ReturnsExistingUrl() {
		// Given
		Long userId = 7L;
		Long orderId = 1003L;
		Order order = createOrder(orderId, userId, OrderStatus.PENDING, "cs_existing_1003");
		CheckoutSessionResult existingSession = new CheckoutSessionResult(
				"cs_existing_1003",
				"https://checkout.stripe.com/c/pay/cs_existing_1003"
		);
		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);
		given(checkoutSessionGateway.findReusableCheckoutSession(order)).willReturn(Optional.of(existingSession));

		// When
		PaymentSessionResponse response = paymentService.createCheckoutSession(userId, orderId);

		// Then
		assertThat(response.orderId()).isEqualTo(orderId);
		assertThat(response.sessionId()).isEqualTo("cs_existing_1003");
		assertThat(response.paymentUrl()).isEqualTo("https://checkout.stripe.com/c/pay/cs_existing_1003");

		verify(orderItemRepository, never()).findByOrder_IdWithProduct(any());
		verify(checkoutSessionGateway, never()).createCheckoutSession(any(), any());
		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("createCheckoutSession: existing stale stripe session creates a new session and updates order")
	void createCheckoutSession_ExistingStaleSession_CreatesNewSession() {
		// Given
		Long userId = 7L;
		Long orderId = 1005L;
		Order order = createOrder(orderId, userId, OrderStatus.PENDING, "cs_old_1005");
		OrderItem orderItem = createOrderItem(12L, "MacBook", new BigDecimal("45990000"), 1);
		CheckoutSessionResult newSession = new CheckoutSessionResult(
				"cs_new_1005",
				"https://checkout.stripe.com/c/pay/cs_new_1005"
		);

		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);
		given(checkoutSessionGateway.findReusableCheckoutSession(order)).willReturn(Optional.empty());
		given(orderItemRepository.findByOrder_IdWithProduct(orderId)).willReturn(List.of(orderItem));
		given(checkoutSessionGateway.createCheckoutSession(order, List.of(orderItem))).willReturn(newSession);
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

		// When
		PaymentSessionResponse response = paymentService.createCheckoutSession(userId, orderId);

		// Then
		assertThat(response.orderId()).isEqualTo(orderId);
		assertThat(response.sessionId()).isEqualTo("cs_new_1005");
		assertThat(response.paymentUrl()).isEqualTo("https://checkout.stripe.com/c/pay/cs_new_1005");

		verify(orderRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getStripeSessionId()).isEqualTo("cs_new_1005");
	}

	@Test
	@DisplayName("createCheckoutSession: order without items throws INVALID_INPUT")
	void createCheckoutSession_EmptyOrderItems_ThrowsInvalidInput() {
		// Given
		Long userId = 7L;
		Long orderId = 1004L;
		Order order = createOrder(orderId, userId, OrderStatus.PENDING, null);
		given(orderService.getOrderOwnedByUser(userId, orderId)).willReturn(order);
		given(checkoutSessionGateway.findReusableCheckoutSession(order)).willReturn(Optional.empty());
		given(orderItemRepository.findByOrder_IdWithProduct(orderId)).willReturn(List.of());

		// When + Then
		assertThatThrownBy(() -> paymentService.createCheckoutSession(userId, orderId))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);

		verify(checkoutSessionGateway, never()).createCheckoutSession(any(), any());
		verify(orderRepository, never()).save(any());
	}

	private Order createOrder(Long orderId, Long userId, OrderStatus status, String stripeSessionId) {
		User user = new User();
		user.setId(userId);

		Order order = new Order();
		order.setId(orderId);
		order.setUser(user);
		order.setStatus(status);
		order.setStripeSessionId(stripeSessionId);
		order.setShippingAddress("Test address");
		order.setTotalAmount(new BigDecimal("29990000"));
		order.setDiscountAmount(BigDecimal.ZERO);
		return order;
	}

	private OrderItem createOrderItem(Long productId, String productName, BigDecimal unitPrice, int quantity) {
		Product product = new Product();
		product.setId(productId);
		product.setName(productName);

		OrderItem orderItem = new OrderItem();
		orderItem.setProduct(product);
		orderItem.setQuantity(quantity);
		orderItem.setPriceAtTime(unitPrice);
		return orderItem;
	}
}
