package com.affismart.mall.modules.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateOrderRequest(
		@NotBlank(message = "Shipping address is required")
		String shippingAddress,

		@NotEmpty(message = "Order items are required")
		List<@Valid CreateOrderItemRequest> items,

		String refCode
) {
}
