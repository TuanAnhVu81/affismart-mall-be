package com.affismart.mall.modules.order.service;

import com.affismart.mall.modules.order.entity.Order;

public interface OrderPaymentGateway {

	void refundForOrder(Order order);
}
