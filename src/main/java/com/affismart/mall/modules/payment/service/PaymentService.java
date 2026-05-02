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
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PaymentService {

	private final OrderService orderService;
	private final OrderItemRepository orderItemRepository;
	private final OrderRepository orderRepository;
	private final CheckoutSessionGateway checkoutSessionGateway;

	public PaymentService(
			OrderService orderService,
			OrderItemRepository orderItemRepository,
			OrderRepository orderRepository,
			CheckoutSessionGateway checkoutSessionGateway
	) {
		this.orderService = orderService;
		this.orderItemRepository = orderItemRepository;
		this.orderRepository = orderRepository;
		this.checkoutSessionGateway = checkoutSessionGateway;
	}

	public PaymentSessionResponse createCheckoutSession(Long userId, Long orderId) {
		Order order = orderService.getOrderOwnedByUser(userId, orderId);
		validateOrderForPaymentSession(order);

		Optional<CheckoutSessionResult> reusableSession = checkoutSessionGateway.findReusableCheckoutSession(order);
		if (reusableSession.isPresent()) {
			return toResponse(order.getId(), reusableSession.get());
		}

		List<OrderItem> orderItems = orderItemRepository.findByOrder_IdWithProduct(order.getId());
		if (orderItems.isEmpty()) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Cannot create payment session for empty order");
		}

		CheckoutSessionResult sessionResult = checkoutSessionGateway.createCheckoutSession(order, orderItems);
		if (sessionResult == null
				|| !StringUtils.hasText(sessionResult.sessionId())
				|| !StringUtils.hasText(sessionResult.checkoutUrl())) {
			throw new AppException(
					ErrorCode.PAYMENT_SESSION_CREATION_FAILED,
					"Payment provider returned an invalid checkout session"
			);
		}

		order.setStripeSessionId(sessionResult.sessionId());
		Order savedOrder = orderRepository.save(order);
		return toResponse(savedOrder.getId(), sessionResult);
	}

	private PaymentSessionResponse toResponse(Long orderId, CheckoutSessionResult sessionResult) {
		return new PaymentSessionResponse(orderId, sessionResult.sessionId(), sessionResult.checkoutUrl());
	}

	private void validateOrderForPaymentSession(Order order) {
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new AppException(
					ErrorCode.ORDER_PAYMENT_NOT_ALLOWED,
					"Only PENDING orders can create payment session"
			);
		}
	}
}
