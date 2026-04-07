package com.affismart.mall.modules.order.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import com.affismart.mall.modules.order.dto.request.CreateOrderRequest;
import com.affismart.mall.modules.order.dto.response.CreateOrderResponse;
import com.affismart.mall.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders", description = "Endpoints for checkout and order management")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@Operation(summary = "Create order from cart (Customer only)")
	@PreAuthorize("hasRole('CUSTOMER')")
	@PostMapping
	public ApiResponse<CreateOrderResponse> createOrder(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreateOrderRequest request
	) {
		return ApiResponse.success(
				"Order created successfully",
				orderService.createOrder(principal.getUserId(), request)
		);
	}
}
