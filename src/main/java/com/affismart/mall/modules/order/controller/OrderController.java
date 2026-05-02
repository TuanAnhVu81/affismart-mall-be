package com.affismart.mall.modules.order.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.common.response.PageResponse;
import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import com.affismart.mall.modules.order.dto.request.CreateOrderRequest;
import com.affismart.mall.modules.order.dto.request.UpdateOrderStatusRequest;
import com.affismart.mall.modules.order.dto.response.CreateOrderResponse;
import com.affismart.mall.modules.order.dto.response.OrderDetailResponse;
import com.affismart.mall.modules.order.dto.response.OrderSummaryResponse;
import com.affismart.mall.modules.order.service.OrderService;
import com.affismart.mall.modules.order.service.OrderStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import org.springframework.validation.annotation.Validated;
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
import org.springframework.format.annotation.DateTimeFormat;

@Tag(name = "Orders", description = "Endpoints for checkout and order management")
@Validated
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
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(name = "sort_by", defaultValue = "createdAt") String sortBy,
			@RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir
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
			@PathVariable @Positive Long id
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
			@PathVariable @Positive Long id
	) {
		orderStatusService.cancelMyOrder(principal.getUserId(), id);
		return ApiResponse.success("Order cancelled successfully");
	}

	@Operation(summary = "Get all orders for admin (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	public ApiResponse<PageResponse<OrderSummaryResponse>> getOrdersForAdmin(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(name = "sort_by", defaultValue = "createdAt") String sortBy,
			@RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir,
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(name = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
			@RequestParam(name = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo
	) {
		return ApiResponse.success(
				"Orders retrieved successfully",
				orderService.getOrdersForAdmin(page, size, sortBy, sortDir, status, createdFrom, createdTo)
		);
	}

	@Operation(summary = "Get order detail for admin (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/{id}")
	public ApiResponse<OrderDetailResponse> getOrderDetailForAdmin(@PathVariable @Positive Long id) {
		return ApiResponse.success(
				"Order retrieved successfully",
				orderService.getOrderDetailForAdmin(id)
		);
	}

	@Operation(summary = "Update order status for admin (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}/status")
	public ApiResponse<OrderDetailResponse> updateOrderStatusByAdmin(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateOrderStatusRequest request
	) {
		orderStatusService.updateOrderStatusByAdmin(id, request.status());
		return ApiResponse.success(
				"Order status updated successfully",
				orderService.getOrderDetailForAdmin(id)
		);
	}
}
