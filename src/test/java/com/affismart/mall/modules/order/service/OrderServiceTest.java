package com.affismart.mall.modules.order.service;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.dto.request.CreateOrderItemRequest;
import com.affismart.mall.modules.order.dto.request.CreateOrderRequest;
import com.affismart.mall.modules.order.dto.response.CreateOrderResponse;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.entity.OrderItem;
import com.affismart.mall.modules.order.repository.AffiliateAccountLookupRepository;
import com.affismart.mall.modules.order.repository.OrderRepository;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.product.repository.ProductRepository;
import com.affismart.mall.modules.user.entity.User;
import com.affismart.mall.modules.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private AffiliateAccountLookupRepository affiliateAccountLookupRepository;

	@InjectMocks
	private OrderService orderService;

	@Captor
	private ArgumentCaptor<Order> orderCaptor;

	// =========================================================
	// createOrder()
	// =========================================================

	@Test
	@DisplayName("createOrder: Happy Path - creates pending order, decrements stock, and snapshots item prices")
	void createOrder_ValidRequest_PersistsOrderWithSnapshots() {
		// Given
		Long userId = 7L;
		CreateOrderRequest request = new CreateOrderRequest(
				" 123 Nguyen Trai, HCM ",
				List.of(
						new CreateOrderItemRequest(101L, 2),
						new CreateOrderItemRequest(202L, 1)
				),
				"AFFI-01"
		);
		User user = createUser(userId);
		Product productOne = createProduct(101L, new BigDecimal("100.00"), 5, true);
		Product productTwo = createProduct(202L, new BigDecimal("250.00"), 10, true);

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of(productOne, productTwo));
		given(affiliateAccountLookupRepository.findApprovedAccountIdByRefCode("AFFI-01")).willReturn(Optional.of(99L));
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
			Order order = invocation.getArgument(0);
			order.setId(55L);
			return order;
		});

		// When
		CreateOrderResponse result = orderService.createOrder(userId, request);

		// Then
		assertThat(result.orderId()).isEqualTo(55L);
		verify(orderRepository).save(orderCaptor.capture());
		Order savedOrder = orderCaptor.getValue();

		assertThat(savedOrder.getUser()).isSameAs(user);
		assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(savedOrder.getShippingAddress()).isEqualTo("123 Nguyen Trai, HCM");
		assertThat(savedOrder.getDiscountAmount()).isEqualByComparingTo("0.00");
		assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("450.00");
		assertThat(savedOrder.getAffiliateAccountId()).isEqualTo(99L);
		assertThat(savedOrder.getOrderItems()).hasSize(2);

		Map<Long, OrderItem> itemsByProductId = savedOrder.getOrderItems().stream()
				.collect(java.util.stream.Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));
		assertThat(itemsByProductId.get(101L).getQuantity()).isEqualTo(2);
		assertThat(itemsByProductId.get(101L).getPriceAtTime()).isEqualByComparingTo("100.00");
		assertThat(itemsByProductId.get(202L).getQuantity()).isEqualTo(1);
		assertThat(itemsByProductId.get(202L).getPriceAtTime()).isEqualByComparingTo("250.00");

		assertThat(productOne.getStockQuantity()).isEqualTo(3);
		assertThat(productTwo.getStockQuantity()).isEqualTo(9);
	}

	@Test
	@DisplayName("createOrder: Exception - duplicate product ids in request throws INVALID_INPUT")
	void createOrder_DuplicateProductIds_ThrowsInvalidInput() {
		// Given
		CreateOrderRequest request = new CreateOrderRequest(
				"address",
				List.of(
						new CreateOrderItemRequest(1L, 1),
						new CreateOrderItemRequest(1L, 2)
				),
				null
		);

		// When + Then
		assertThatThrownBy(() -> orderService.createOrder(1L, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);

		verify(userRepository, never()).findById(any());
		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("createOrder: Exception - missing user throws USER_NOT_FOUND")
	void createOrder_UserNotFound_ThrowsUserNotFound() {
		// Given
		Long userId = 404L;
		CreateOrderRequest request = new CreateOrderRequest(
				"address",
				List.of(new CreateOrderItemRequest(1L, 1)),
				null
		);
		given(userRepository.findById(userId)).willReturn(Optional.empty());

		// When + Then
		assertThatThrownBy(() -> orderService.createOrder(userId, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);

		verify(productRepository, never()).findAllByIdInForUpdate(anyCollection());
	}

	@Test
	@DisplayName("createOrder: Exception - one or more product ids not found throws PRODUCT_NOT_FOUND")
	void createOrder_ProductMissing_ThrowsProductNotFound() {
		// Given
		Long userId = 1L;
		CreateOrderRequest request = new CreateOrderRequest(
				"address",
				List.of(
						new CreateOrderItemRequest(10L, 1),
						new CreateOrderItemRequest(20L, 1)
				),
				null
		);
		User user = createUser(userId);
		Product existingProduct = createProduct(10L, new BigDecimal("50.00"), 5, true);

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of(existingProduct));

		// When + Then
		assertThatThrownBy(() -> orderService.createOrder(userId, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("createOrder: Exception - inactive product is rejected as PRODUCT_NOT_FOUND")
	void createOrder_InactiveProduct_ThrowsProductNotFound() {
		// Given
		Long userId = 1L;
		CreateOrderRequest request = new CreateOrderRequest(
				"address",
				List.of(new CreateOrderItemRequest(10L, 1)),
				null
		);
		User user = createUser(userId);
		Product inactiveProduct = createProduct(10L, new BigDecimal("50.00"), 5, false);

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of(inactiveProduct));

		// When + Then
		assertThatThrownBy(() -> orderService.createOrder(userId, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("createOrder: Exception - insufficient stock throws INVALID_INPUT")
	void createOrder_InsufficientStock_ThrowsInvalidInput() {
		// Given
		Long userId = 1L;
		CreateOrderRequest request = new CreateOrderRequest(
				"address",
				List.of(new CreateOrderItemRequest(10L, 3)),
				null
		);
		User user = createUser(userId);
		Product product = createProduct(10L, new BigDecimal("50.00"), 2, true);

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of(product));

		// When + Then
		assertThatThrownBy(() -> orderService.createOrder(userId, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);

		verify(orderRepository, never()).save(any());
	}

	@Test
	@DisplayName("createOrder: blank refCode does not trigger affiliate lookup and keeps affiliateAccountId null")
	void createOrder_BlankRefCode_DoesNotLookupAffiliate() {
		// Given
		Long userId = 1L;
		CreateOrderRequest request = new CreateOrderRequest(
				"address",
				List.of(new CreateOrderItemRequest(10L, 1)),
				"   "
		);
		User user = createUser(userId);
		Product product = createProduct(10L, new BigDecimal("20.00"), 5, true);

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(productRepository.findAllByIdInForUpdate(anyCollection())).willReturn(List.of(product));
		given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
			Order order = invocation.getArgument(0);
			order.setId(70L);
			return order;
		});

		// When
		orderService.createOrder(userId, request);

		// Then
		verify(affiliateAccountLookupRepository, never()).findApprovedAccountIdByRefCode(any());
		verify(orderRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getAffiliateAccountId()).isNull();
	}

	// =========================================================
	// Private Helper Methods
	// =========================================================

	private User createUser(Long id) {
		User user = new User();
		user.setId(id);
		user.setEmail("user@example.com");
		user.setPasswordHash("hash");
		user.setFullName("User");
		return user;
	}

	private Product createProduct(Long id, BigDecimal price, int stockQuantity, boolean active) {
		Product product = new Product();
		product.setId(id);
		product.setName("Product-" + id);
		product.setSku("SKU-" + id);
		product.setSlug("product-" + id);
		product.setPrice(price);
		product.setStockQuantity(stockQuantity);
		product.setActive(active);
		return product;
	}
}
