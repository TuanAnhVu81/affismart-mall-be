package com.affismart.mall.integration.stripe;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.entity.OrderItem;
import com.affismart.mall.modules.payment.model.CheckoutSessionResult;
import com.affismart.mall.modules.payment.service.CheckoutSessionGateway;
import com.affismart.mall.modules.payment.service.PaymentRedirectService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Primary
@ConditionalOnProperty(prefix = "app.payment.stripe", name = "enabled", havingValue = "true")
public class StripeCheckoutSessionGateway implements CheckoutSessionGateway {

	private static final Logger log = LoggerFactory.getLogger(StripeCheckoutSessionGateway.class);

	private final StripeProperties stripeProperties;
	private final PaymentRedirectService paymentRedirectService;
	private final String currency;

	public StripeCheckoutSessionGateway(StripeProperties stripeProperties, PaymentRedirectService paymentRedirectService) {
		this.stripeProperties = stripeProperties;
		this.paymentRedirectService = paymentRedirectService;
		validateRequiredProperty(stripeProperties.getSecretKey(), "app.payment.stripe.secret-key");
		validateRequiredProperty(stripeProperties.getRedirectBaseUrl(), "app.payment.stripe.redirect-base-url");
		validateRequiredProperty(stripeProperties.getSuccessUrl(), "app.payment.stripe.success-url");
		validateRequiredProperty(stripeProperties.getCancelUrl(), "app.payment.stripe.cancel-url");
		this.currency = normalizeCurrencyCode(stripeProperties.getCurrency());
	}

	@Override
	public Optional<CheckoutSessionResult> findReusableCheckoutSession(Order order) {
		if (!StringUtils.hasText(order.getStripeSessionId())) {
			return Optional.empty();
		}

		try {
			Session existingSession = Session.retrieve(
					order.getStripeSessionId(),
					RequestOptions.builder().setApiKey(stripeProperties.getSecretKey()).build()
			);

			if (!isSessionBoundToOrder(existingSession, order.getId())) {
				log.warn(
						"Skip reusing Stripe session because metadata mismatch. order_id={}, session_id={}",
						order.getId(),
						order.getStripeSessionId()
				);
				return Optional.empty();
			}

			if (!isReusable(existingSession)) {
				return Optional.empty();
			}

			if (!StringUtils.hasText(existingSession.getUrl())) {
				return Optional.empty();
			}
			return Optional.of(new CheckoutSessionResult(existingSession.getId(), existingSession.getUrl()));
		} catch (StripeException exception) {
			log.warn(
					"Unable to retrieve existing Stripe session for order_id={}, session_id={}",
					order.getId(),
					order.getStripeSessionId(),
					exception
			);
			return Optional.empty();
		}
	}

	@Override
	public CheckoutSessionResult createCheckoutSession(Order order, List<OrderItem> orderItems) {
		SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.PAYMENT)
				.setSuccessUrl(paymentRedirectService.getStripeSuccessReturnUrl())
				.setCancelUrl(paymentRedirectService.getStripeCancelReturnUrl())
				.putMetadata("orderId", String.valueOf(order.getId()))
				.putMetadata("userId", String.valueOf(order.getUser().getId()));

		for (OrderItem orderItem : orderItems) {
			String productName = resolveProductName(orderItem);
			long unitAmount = toMinorAmount(orderItem.getPriceAtTime(), currency);

			sessionBuilder.addLineItem(
					SessionCreateParams.LineItem.builder()
							.setQuantity(orderItem.getQuantity().longValue())
							.setPriceData(
									SessionCreateParams.LineItem.PriceData.builder()
											.setCurrency(currency)
											.setUnitAmount(unitAmount)
											.setProductData(
													SessionCreateParams.LineItem.PriceData.ProductData.builder()
															.setName(productName)
															.build()
											)
											.build()
							)
							.build()
			);
		}

		try {
			Session session = Session.create(
					sessionBuilder.build(),
					RequestOptions.builder().setApiKey(stripeProperties.getSecretKey()).build()
			);

			if (!StringUtils.hasText(session.getId()) || !StringUtils.hasText(session.getUrl())) {
				throw new AppException(
						ErrorCode.PAYMENT_SESSION_CREATION_FAILED,
						"Stripe returned an invalid checkout session payload"
				);
			}
			return new CheckoutSessionResult(session.getId(), session.getUrl());
		} catch (StripeException exception) {
			log.error("Failed to create Stripe checkout session for order_id={}", order.getId(), exception);
			throw new AppException(
					ErrorCode.PAYMENT_SESSION_CREATION_FAILED,
					"Failed to create Stripe checkout session"
			);
		}
	}

	private long toMinorAmount(BigDecimal amount, String currencyCode) {
		if (amount == null) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Order item amount is required");
		}

		int fractionDigits = Currency.getInstance(currencyCode.toUpperCase(Locale.ROOT)).getDefaultFractionDigits();
		BigDecimal minor = amount.movePointRight(Math.max(fractionDigits, 0)).setScale(0, RoundingMode.HALF_UP);
		if (minor.signum() <= 0) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Order item amount must be greater than zero");
		}

		try {
			return minor.longValueExact();
		} catch (ArithmeticException exception) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Order item amount is too large for payment provider");
		}
	}

	private boolean isSessionBoundToOrder(Session session, Long orderId) {
		Map<String, String> metadata = session.getMetadata();
		if (metadata == null || metadata.isEmpty()) {
			return false;
		}
		String sessionOrderId = metadata.get("orderId");
		return String.valueOf(orderId).equals(sessionOrderId);
	}

	private boolean isReusable(Session session) {
		String status = normalize(session.getStatus());
		String paymentStatus = normalize(session.getPaymentStatus());
		return "open".equals(status) && !"paid".equals(paymentStatus);
	}

	private String normalize(String value) {
		if (!StringUtils.hasText(value)) {
			return "";
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeCurrencyCode(String configuredCurrency) {
		String currency = StringUtils.hasText(configuredCurrency) ? configuredCurrency.trim() : "vnd";
		try {
			return Currency.getInstance(currency.toUpperCase(Locale.ROOT))
					.getCurrencyCode()
					.toLowerCase(Locale.ROOT);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("Unsupported currency code: " + currency, exception);
		}
	}

	private String resolveProductName(OrderItem orderItem) {
		if (orderItem.getProduct() != null && StringUtils.hasText(orderItem.getProduct().getName())) {
			return orderItem.getProduct().getName().trim();
		}
		if (orderItem.getProduct() != null && orderItem.getProduct().getId() != null) {
			return "Product #" + orderItem.getProduct().getId();
		}
		return "Order Item";
	}

	private void validateRequiredProperty(String value, String propertyName) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalStateException("Missing required property: " + propertyName);
		}
	}
}
