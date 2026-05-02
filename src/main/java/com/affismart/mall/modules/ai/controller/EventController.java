package com.affismart.mall.modules.ai.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.modules.ai.dto.request.LogRecommendationEventRequest;
import com.affismart.mall.modules.ai.service.EventLoggingService;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Events", description = "Endpoints for ingesting recommendation events")
@Validated
@RestController
@RequestMapping("/api/v1/ai")
public class EventController {

	private final EventLoggingService eventLoggingService;

	public EventController(EventLoggingService eventLoggingService) {
		this.eventLoggingService = eventLoggingService;
	}

	@Operation(summary = "Log recommendation event (Public, optional auth)")
	@SecurityRequirements
	@PostMapping("/events")
	public ApiResponse<Void> logEvent(
			@Valid @RequestBody LogRecommendationEventRequest request,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		eventLoggingService.submitEvent(principal != null ? principal.getUserId() : null, request);
		return ApiResponse.success("Recommendation event accepted");
	}
}
