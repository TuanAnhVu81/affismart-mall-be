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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStatusService {

	private final OrderService orderService;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ProductRepository productRepository;
	private final OrderPaymentGateway orderPaymentGateway;
	private final CommissionMaintenanceRepository commissionMaintenanceRepository;

	public OrderStatusService(
			OrderService orderService,
			OrderRepository orderRepository,
			OrderItemRepository orderItemRepository,
			ProductRepository productRepository,
			OrderPaymentGateway orderPaymentGateway,
			CommissionMaintenanceRepository commissionMaintenanceRepository
	) {
		this.orderService = orderService;
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.productRepository = productRepository;
		this.orderPaymentGateway = orderPaymentGateway;
		this.commissionMaintenanceRepository = commissionMaintenanceRepository;
	}

	@Transactional
	public void cancelMyOrder(Long userId, Long orderId) {
		Order order = orderService.getOrderOwnedByUser(userId, orderId);
		if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
			throw new AppException(
					ErrorCode.ORDER_CANCELLATION_NOT_ALLOWED,
					"Only PENDING or PAID orders can be cancelled"
			);
		}

		List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(orderId);
		restockProducts(orderItems);

		if (order.getStatus() == OrderStatus.PAID) {
			orderPaymentGateway.refundForOrder(order);
			commissionMaintenanceRepository.rejectPendingCommissionByOrderId(order.getId());
		}

		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
	}

	private void restockProducts(List<OrderItem> orderItems) {
		if (orderItems.isEmpty()) {
			return;
		}

		Set<Long> productIds = orderItems.stream()
				.map(orderItem -> orderItem.getProduct().getId())
				.collect(Collectors.toSet());
		List<Product> products = productRepository.findAllByIdInForUpdate(productIds);
		Map<Long, Product> productById = new HashMap<>();
		for (Product product : products) {
			productById.put(product.getId(), product);
		}

		for (OrderItem orderItem : orderItems) {
			Product product = productById.get(orderItem.getProduct().getId());
			if (product == null) {
				throw new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found while restoring stock");
			}
			product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
		}
	}
}
