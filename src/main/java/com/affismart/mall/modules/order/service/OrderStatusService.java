package com.affismart.mall.modules.order.service;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.repository.CommissionMaintenanceRepository;
import com.affismart.mall.modules.order.repository.OrderRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderStatusService {

	private final OrderRepository orderRepository;
	private final OrderPaymentGateway orderPaymentGateway;
	private final CommissionMaintenanceRepository commissionMaintenanceRepository;
	private final OrderCancellationPersistenceService orderCancellationPersistenceService;

	public OrderStatusService(
			OrderRepository orderRepository,
			OrderPaymentGateway orderPaymentGateway,
			CommissionMaintenanceRepository commissionMaintenanceRepository,
			OrderCancellationPersistenceService orderCancellationPersistenceService
	) {
		this.orderRepository = orderRepository;
		this.orderPaymentGateway = orderPaymentGateway;
		this.commissionMaintenanceRepository = commissionMaintenanceRepository;
		this.orderCancellationPersistenceService = orderCancellationPersistenceService;
	}

	public void cancelMyOrder(Long userId, Long orderId) {
		Order order = orderCancellationPersistenceService.getCancellableOrder(userId, orderId);
		if (order.getStatus() == OrderStatus.PAID) {
			orderPaymentGateway.refundForOrder(order);
			orderCancellationPersistenceService.finalizePaidOrderCancellation(userId, orderId);
			return;
		}

		orderCancellationPersistenceService.cancelPendingOrder(userId, orderId);
	}

	@Transactional
	public void updateOrderStatusByAdmin(Long orderId, OrderStatus targetStatus) {
		validateAdminTargetStatus(targetStatus);
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

		if (order.getStatus() == targetStatus) {
			return;
		}

		if (!order.getStatus().canTransitionTo(targetStatus)) {
			throw new AppException(
					ErrorCode.ORDER_STATUS_TRANSITION_NOT_ALLOWED,
					"Cannot transition order status from " + order.getStatus() + " to " + targetStatus
			);
		}

		order.setStatus(targetStatus);
		orderRepository.save(order);
		handleCommissionResolutionOnStatusChange(order);
	}

	@Transactional
	public Order markOrderPaidByWebhook(Long orderId, String stripeSessionId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

		if (isPaidOrHigher(order.getStatus())) {
			return order;
		}

		if (order.getStatus() != OrderStatus.PENDING) {
			return order;
		}

		order.setStatus(OrderStatus.PAID);
		if (StringUtils.hasText(stripeSessionId)) {
			order.setStripeSessionId(stripeSessionId.trim());
		}
		return orderRepository.save(order);
	}

	private void validateAdminTargetStatus(OrderStatus targetStatus) {
		Set<OrderStatus> allowedTargets = Set.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DONE);
		if (targetStatus == null || !allowedTargets.contains(targetStatus)) {
			throw new AppException(
					ErrorCode.INVALID_INPUT,
					"Admin can only update order status to CONFIRMED, SHIPPED, or DONE"
			);
		}
	}

	private void handleCommissionResolutionOnStatusChange(Order order) {
		if (order.getStatus() == OrderStatus.DONE) {
			commissionMaintenanceRepository.approvePendingCommissionAndAddBalanceByOrderId(order.getId());
		} else if (order.getStatus() == OrderStatus.CANCELLED) {
			commissionMaintenanceRepository.rejectPendingCommissionByOrderId(order.getId());
		}
	}

	private boolean isPaidOrHigher(OrderStatus status) {
		return status == OrderStatus.PAID
				|| status == OrderStatus.CONFIRMED
				|| status == OrderStatus.SHIPPED
				|| status == OrderStatus.DONE;
	}
}
