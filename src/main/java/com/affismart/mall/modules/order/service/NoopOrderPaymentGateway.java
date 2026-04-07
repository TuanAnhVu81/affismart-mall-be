package com.affismart.mall.modules.order.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.order.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NoopOrderPaymentGateway implements OrderPaymentGateway {

	private static final Logger log = LoggerFactory.getLogger(NoopOrderPaymentGateway.class);

	@Override
	public void refundForOrder(Order order) {
		if (!StringUtils.hasText(order.getStripeSessionId())) {
			throw new AppException(
					ErrorCode.PAYMENT_REFUND_FAILED,
					"Cannot cancel paid order without stripe session id"
			);
		}

		// Phase 4 will replace this adapter with real Stripe refund implementation.
		log.info(
				"Skipping external refund for paid order {} in NOOP gateway. sessionId={}",
				order.getId(),
				order.getStripeSessionId()
		);
	}
}
