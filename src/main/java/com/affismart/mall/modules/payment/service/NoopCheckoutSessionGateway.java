package com.affismart.mall.modules.payment.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.entity.OrderItem;
import com.affismart.mall.modules.payment.model.CheckoutSessionResult;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NoopCheckoutSessionGateway implements CheckoutSessionGateway {

	@Override
	public Optional<CheckoutSessionResult> findReusableCheckoutSession(Order order) {
		return Optional.empty();
	}

	@Override
	public CheckoutSessionResult createCheckoutSession(Order order, List<OrderItem> orderItems) {
		throw new AppException(
				ErrorCode.PAYMENT_GATEWAY_NOT_CONFIGURED,
				"Stripe checkout is not configured. Please enable app.payment.stripe and provide secret key"
		);
	}
}
