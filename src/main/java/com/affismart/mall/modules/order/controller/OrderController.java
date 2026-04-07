package com.affismart.mall.modules.order.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.common.response.PageResponse;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import com.affismart.mall.modules.order.dto.request.CreateOrderRequest;
import com.affismart.mall.modules.order.dto.response.CreateOrderResponse;
import com.affismart.mall.modules.order.dto.response.OrderDetailResponse;
import com.affismart.mall.modules.order.dto.response.OrderSummaryResponse;
import com.affismart.mall.modules.order.service.OrderService;
import com.affismart.mall.modules.order.service.OrderStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders", description = "Endpoints for checkout and order management")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final OrderService orderService;
	private final OrderStatusService orderStatusService;

	public OrderController(OrderService orderService, OrderStatusService orderStatusService) {
		this.orderService = orderService;
		this.orderStatusService = orderStatusService;
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

	@Operation(summary = "Get my orders (Customer only)")
	@PreAuthorize("hasRole('CUSTOMER')")
	@GetMapping("/my")
	public ApiResponse<PageResponse<OrderSummaryResponse>> getMyOrders(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir
	) {
		return ApiResponse.success(
				"My orders retrieved successfully",
				orderService.getMyOrders(principal.getUserId(), page, size, sortBy, sortDir)
		);
	}

	@Operation(summary = "Get my order detail (Customer only)")
	@PreAuthorize("hasRole('CUSTOMER')")
	@GetMapping("/my/{id}")
	public ApiResponse<OrderDetailResponse> getMyOrderDetail(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id
	) {
		return ApiResponse.success(
				"My order detail retrieved successfully",
				orderService.getMyOrderDetail(principal.getUserId(), id)
		);
	}

	@Operation(summary = "Cancel my order (Customer only)")
	@PreAuthorize("hasRole('CUSTOMER')")
	@PutMapping("/my/{id}/cancel")
	public ApiResponse<Void> cancelMyOrder(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id
	) {
		orderStatusService.cancelMyOrder(principal.getUserId(), id);
		return ApiResponse.success("Order cancelled successfully");
	}
}
