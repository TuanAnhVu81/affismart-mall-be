package com.affismart.mall.modules.order.service;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.common.response.PageResponse;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.dto.request.CreateOrderItemRequest;
import com.affismart.mall.modules.order.dto.request.CreateOrderRequest;
import com.affismart.mall.modules.order.dto.response.CreateOrderResponse;
import com.affismart.mall.modules.order.dto.response.OrderDetailResponse;
import com.affismart.mall.modules.order.dto.response.OrderSummaryResponse;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.entity.OrderItem;
import com.affismart.mall.modules.order.repository.AffiliateAccountLookupRepository;
import com.affismart.mall.modules.order.repository.OrderItemRepository;
import com.affismart.mall.modules.order.repository.OrderRepository;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.product.repository.ProductRepository;
import com.affismart.mall.modules.user.entity.User;
import com.affismart.mall.modules.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import com.affismart.mall.modules.order.mapper.OrderMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
	private OrderItemRepository orderItemRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private AffiliateAccountLookupRepository affiliateAccountLookupRepository;

	@Spy
	private OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

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

	@Test
	@DisplayName("getMyOrders: returns paginated order summaries for current user")
	void getMyOrders_ValidInput_ReturnsPageResponse() {
		// Given
		Long userId = 1L;
		Order order = new Order();
		order.setId(501L);
		order.setStatus(OrderStatus.PENDING);
		order.setTotalAmount(new BigDecimal("120.00"));
		order.setShippingAddress("123 Test Street");
		order.setCreatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));
		Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);

		given(orderRepository.findByUser_Id(any(Long.class), any(Pageable.class))).willReturn(page);

		// When
		PageResponse<OrderSummaryResponse> result = orderService.getMyOrders(userId, 0, 10, "createdAt", "desc");

		// Then
		assertThat(result.content()).hasSize(1);
		assertThat(result.content().getFirst().id()).isEqualTo(501L);
		assertThat(result.content().getFirst().status()).isEqualTo("PENDING");
	}

	@Test
	@DisplayName("getMyOrderDetail: returns order detail and item list for owner")
	void getMyOrderDetail_ValidOwner_ReturnsDetail() {
		// Given
		Long userId = 1L;
		Long orderId = 700L;
		Order order = new Order();
		order.setId(orderId);
		order.setStatus(OrderStatus.PENDING);
		order.setTotalAmount(new BigDecimal("200.00"));
		order.setDiscountAmount(BigDecimal.ZERO);
		order.setShippingAddress("Home");
		order.setCreatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));
		order.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 10, 5));

		Product product = createProduct(99L, new BigDecimal("100.00"), 5, true);
		OrderItem orderItem = new OrderItem();
		orderItem.setOrder(order);
		orderItem.setProduct(product);
		orderItem.setQuantity(2);
		orderItem.setPriceAtTime(new BigDecimal("100.00"));

		given(orderRepository.findByIdAndUser_Id(orderId, userId)).willReturn(Optional.of(order));
		given(orderItemRepository.findByOrder_IdWithProduct(orderId)).willReturn(List.of(orderItem));

		// When
		OrderDetailResponse result = orderService.getMyOrderDetail(userId, orderId);

		// Then
		assertThat(result.id()).isEqualTo(orderId);
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().productId()).isEqualTo(99L);
		assertThat(result.items().getFirst().lineTotal()).isEqualByComparingTo("200.00");
	}

	@Test
	@DisplayName("getMyOrderDetail: Exception - order not found for user throws ORDER_NOT_FOUND")
	void getMyOrderDetail_OrderNotFound_ThrowsException() {
		// Given
		Long userId = 1L;
		Long orderId = 999L;

		given(orderRepository.findByIdAndUser_Id(orderId, userId)).willReturn(Optional.empty());

		// When + Then
		assertThatThrownBy(() -> orderService.getMyOrderDetail(userId, orderId))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ORDER_NOT_FOUND);

		verify(orderItemRepository, never()).findByOrder_IdWithProduct(any());
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
