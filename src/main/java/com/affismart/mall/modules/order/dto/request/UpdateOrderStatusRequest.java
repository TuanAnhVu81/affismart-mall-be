package com.affismart.mall.modules.order.dto.request;

import com.affismart.mall.common.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
		@NotNull(message = "Order status is required")
		OrderStatus status
) {
}
