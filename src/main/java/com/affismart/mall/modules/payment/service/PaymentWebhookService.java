package com.affismart.mall.modules.payment.service;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.repository.OrderRepository;
import com.affismart.mall.modules.order.service.CommissionService;
import com.affismart.mall.modules.order.service.OrderStatusService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PaymentWebhookService {

	private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);
	private static final String CHECKOUT_SESSION_COMPLETED_EVENT = "checkout.session.completed";

	private final PaymentWebhookVerifier webhookVerifier;
	private final OrderRepository orderRepository;
	private final OrderStatusService orderStatusService;
	private final CommissionService commissionService;

	public PaymentWebhookService(
			PaymentWebhookVerifier webhookVerifier,
			OrderRepository orderRepository,
			OrderStatusService orderStatusService,
			CommissionService commissionService
	) {
		this.webhookVerifier = webhookVerifier;
		this.orderRepository = orderRepository;
		this.orderStatusService = orderStatusService;
		this.commissionService = commissionService;
	}

	public Event verifyWebhook(String payload, String signatureHeader) {
		if (!StringUtils.hasText(signatureHeader)) {
			throw new AppException(
					ErrorCode.PAYMENT_WEBHOOK_SIGNATURE_INVALID,
					"Missing Stripe-Signature header"
			);
		}

		if (!StringUtils.hasText(payload)) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Webhook payload must not be empty");
		}

		Event event = webhookVerifier.verify(payload, signatureHeader);
		log.info("Stripe webhook verified successfully: event_id={}, type={}", event.getId(), event.getType());
		return event;
	}

	public void handleVerifiedEvent(Event event) {
		if (event == null || !StringUtils.hasText(event.getType())) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Stripe webhook event type is missing");
		}

		if (!CHECKOUT_SESSION_COMPLETED_EVENT.equals(event.getType())) {
			log.debug("Skip unsupported Stripe event type={}", event.getType());
			return;
		}

		Session session = extractCheckoutSession(event);
		Long orderId = extractOrderIdFromMetadata(session.getMetadata());
		processCheckoutSessionCompleted(orderId, session.getId());
	}

	@Transactional
	void processCheckoutSessionCompleted(Long orderId, String stripeSessionId) {
		Order currentOrder = orderRepository.findById(orderId)
				.orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

		if (isPaidOrHigher(currentOrder.getStatus())) {
			log.info("Skip duplicate checkout completion webhook for order_id={} with status={}", orderId, currentOrder.getStatus());
			return;
		}

		if (currentOrder.getStatus() != OrderStatus.PENDING) {
			log.warn(
					"Skip checkout completion webhook because order is not pending. order_id={}, status={}",
					orderId,
					currentOrder.getStatus()
			);
			return;
		}

		Order paidOrder = orderStatusService.markOrderPaidByWebhook(orderId, stripeSessionId);
		commissionService.createPendingCommissionForPaidOrder(paidOrder);
	}

	private Session extractCheckoutSession(Event event) {
		EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
		Optional<StripeObject> dataObject = deserializer.getObject();
		if (dataObject.isEmpty() || !(dataObject.get() instanceof Session session)) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Unsupported Stripe webhook payload object");
		}
		return session;
	}

	private Long extractOrderIdFromMetadata(Map<String, String> metadata) {
		String rawOrderId = metadata != null ? metadata.get("orderId") : null;
		if (!StringUtils.hasText(rawOrderId)) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Missing orderId in Stripe webhook metadata");
		}

		try {
			return Long.valueOf(rawOrderId.trim());
		} catch (NumberFormatException exception) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Invalid orderId in Stripe webhook metadata");
		}
	}

	private boolean isPaidOrHigher(OrderStatus status) {
		return status == OrderStatus.PAID
				|| status == OrderStatus.CONFIRMED
				|| status == OrderStatus.SHIPPED
				|| status == OrderStatus.DONE;
	}
}
