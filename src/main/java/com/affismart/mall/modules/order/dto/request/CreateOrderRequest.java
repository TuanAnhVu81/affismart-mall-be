package com.affismart.mall.modules.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderRequest(
		@NotBlank(message = "Shipping address is required")
		@Size(max = 2000, message = "Shipping address must not exceed 2000 characters")
		String shippingAddress,

		@NotEmpty(message = "Order items are required")
		List<@Valid CreateOrderItemRequest> items,

		@Size(max = 50, message = "Referral code must not exceed 50 characters")
		String refCode
) {
}
