package com.affismart.mall.modules.payment.service;

import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.entity.OrderItem;
import com.affismart.mall.modules.payment.model.CheckoutSessionResult;
import java.util.List;
import java.util.Optional;

public interface CheckoutSessionGateway {

	Optional<CheckoutSessionResult> findReusableCheckoutSession(Order order);

	CheckoutSessionResult createCheckoutSession(Order order, List<OrderItem> orderItems);
}
