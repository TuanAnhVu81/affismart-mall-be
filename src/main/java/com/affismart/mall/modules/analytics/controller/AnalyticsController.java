package com.affismart.mall.modules.analytics.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.modules.analytics.dto.response.AnalyticsDashboardResponse;
import com.affismart.mall.modules.analytics.projection.TopAffiliateProjection;
import com.affismart.mall.modules.analytics.projection.TopProductProjection;
import com.affismart.mall.modules.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics", description = "Admin analytics endpoints")
@Validated
@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@Operation(summary = "Get admin dashboard metrics")
	@GetMapping("/dashboard")
	public ApiResponse<AnalyticsDashboardResponse> getDashboard() {
		return ApiResponse.success(
				"Analytics dashboard retrieved successfully",
				analyticsService.getDashboard()
		);
	}

	@Operation(summary = "Get top selling products")
	@GetMapping("/top-products")
	public ApiResponse<List<TopProductProjection>> getTopProducts(
			@RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer limit
	) {
		return ApiResponse.success(
				"Top products retrieved successfully",
				analyticsService.getTopProducts(limit)
		);
	}

	@Operation(summary = "Get top affiliates by attributed revenue")
	@GetMapping("/top-affiliates")
	public ApiResponse<List<TopAffiliateProjection>> getTopAffiliates(
			@RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer limit
	) {
		return ApiResponse.success(
				"Top affiliates retrieved successfully",
				analyticsService.getTopAffiliates(limit)
		);
	}
}
