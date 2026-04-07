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
import com.affismart.mall.modules.order.mapper.OrderMapper;
import com.affismart.mall.modules.order.repository.AffiliateAccountLookupRepository;
import com.affismart.mall.modules.order.repository.OrderItemRepository;
import com.affismart.mall.modules.order.repository.OrderRepository;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.product.repository.ProductRepository;
import com.affismart.mall.modules.user.entity.User;
import com.affismart.mall.modules.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderService {

	private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final AffiliateAccountLookupRepository affiliateAccountLookupRepository;
	private final OrderMapper orderMapper;

	public OrderService(
			OrderRepository orderRepository,
			OrderItemRepository orderItemRepository,
			ProductRepository productRepository,
			UserRepository userRepository,
			AffiliateAccountLookupRepository affiliateAccountLookupRepository,
			OrderMapper orderMapper
	) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
		this.affiliateAccountLookupRepository = affiliateAccountLookupRepository;
		this.orderMapper = orderMapper;
	}

	@Transactional
	public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
		validateNoDuplicateProducts(request.items());

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

		Map<Long, Product> productById = loadProductsForCheckout(request.items());
		Order order = initializeOrder(user, request);

		BigDecimal totalAmount = ZERO_AMOUNT;
		for (CreateOrderItemRequest itemRequest : request.items()) {
			Product product = productById.get(itemRequest.productId());
			validateAvailableForCheckout(product, itemRequest.quantity());
			product.setStockQuantity(product.getStockQuantity() - itemRequest.quantity());

			OrderItem orderItem = new OrderItem();
			orderItem.setProduct(product);
			orderItem.setQuantity(itemRequest.quantity());
			orderItem.setPriceAtTime(product.getPrice());
			order.addOrderItem(orderItem);

			totalAmount = totalAmount.add(
					product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity().longValue()))
			);
		}

		if (totalAmount.compareTo(ZERO_AMOUNT) <= 0) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Order total amount must be greater than zero");
		}

		order.setTotalAmount(totalAmount);
		Order savedOrder = orderRepository.save(order);
		return orderMapper.toCreateOrderResponse(savedOrder);
	}

	@Transactional(readOnly = true)
	public PageResponse<OrderSummaryResponse> getMyOrders(Long userId, int page, int size, String sortBy, String sortDir) {
		Pageable pageable = PageRequest.of(
				page,
				size,
				Sort.by(resolveDirection(sortDir), normalizeSortProperty(sortBy))
		);

		Page<OrderSummaryResponse> responsePage = orderRepository.findByUser_Id(userId, pageable)
				.map(orderMapper::toOrderSummaryResponse);
		return PageResponse.from(responsePage);
	}

	@Transactional(readOnly = true)
	public OrderDetailResponse getMyOrderDetail(Long userId, Long orderId) {
		Order order = getOrderOwnedByUser(userId, orderId);
		List<OrderItem> items = orderItemRepository.findByOrder_IdWithProduct(order.getId());
		return orderMapper.toOrderDetailResponse(order, items);
	}

	@Transactional(readOnly = true)
	public Order getOrderOwnedByUser(Long userId, Long orderId) {
		return orderRepository.findByIdAndUser_Id(orderId, userId)
				.orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
	}

	private void validateNoDuplicateProducts(List<CreateOrderItemRequest> items) {
		Set<Long> ids = new LinkedHashSet<>();
		for (CreateOrderItemRequest item : items) {
			if (!ids.add(item.productId())) {
				throw new AppException(ErrorCode.INVALID_INPUT, "Duplicate productId in order items is not allowed");
			}
		}
	}

	private Map<Long, Product> loadProductsForCheckout(List<CreateOrderItemRequest> items) {
		Set<Long> productIds = items.stream()
				.map(CreateOrderItemRequest::productId)
				.collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

		List<Product> products = productRepository.findAllByIdInForUpdate(productIds);
		if (products.size() != productIds.size()) {
			throw new AppException(ErrorCode.PRODUCT_NOT_FOUND, "One or more products were not found");
		}

		Map<Long, Product> productById = new HashMap<>();
		for (Product product : products) {
			productById.put(product.getId(), product);
		}
		return productById;
	}

	private Order initializeOrder(User user, CreateOrderRequest request) {
		Order order = new Order();
		order.setUser(user);
		order.setStatus(OrderStatus.PENDING);
		order.setShippingAddress(request.shippingAddress().trim());
		order.setDiscountAmount(ZERO_AMOUNT);
		order.setAffiliateAccountId(resolveAffiliateAccountId(request.refCode()));
		return order;
	}

	private void validateAvailableForCheckout(Product product, Integer requestedQuantity) {
		if (product == null || !product.isActive()) {
			throw new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Product is not available for checkout");
		}

		if (requestedQuantity > product.getStockQuantity()) {
			throw new AppException(
					ErrorCode.INVALID_INPUT,
					"Insufficient stock for product id " + product.getId()
			);
		}
	}

	private Long resolveAffiliateAccountId(String refCode) {
		if (!StringUtils.hasText(refCode)) {
			return null;
		}
		return affiliateAccountLookupRepository.findApprovedAccountIdByRefCode(refCode.trim())
				.orElse(null);
	}

	private Sort.Direction resolveDirection(String sortDir) {
		return "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
	}

	private String normalizeSortProperty(String sortBy) {
		if (!StringUtils.hasText(sortBy)) {
			return "createdAt";
		}
		return switch (sortBy.trim().toLowerCase(Locale.ROOT)) {
			case "id" -> "id";
			case "status" -> "status";
			case "totalamount", "total_amount" -> "totalAmount";
			case "updatedat", "updated_at" -> "updatedAt";
			default -> "createdAt";
		};
	}
}
