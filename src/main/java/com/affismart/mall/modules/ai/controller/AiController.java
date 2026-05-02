package com.affismart.mall.modules.ai.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.modules.ai.dto.request.AiChatRequest;
import com.affismart.mall.modules.ai.dto.response.AiChatResponse;
import com.affismart.mall.modules.ai.dto.response.AiRecommendationResponse;
import com.affismart.mall.modules.ai.service.AiProxyService;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "Endpoints for AI recommendations and chatbot")
@Validated
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

	private final AiProxyService aiProxyService;

	public AiController(AiProxyService aiProxyService) {
		this.aiProxyService = aiProxyService;
	}

	@Operation(summary = "Get homepage recommendations (Public, optional auth)")
	@SecurityRequirements
	@GetMapping("/recommendations")
	public ApiResponse<AiRecommendationResponse> getHomepageRecommendations(
			@RequestParam(name = "session_id", required = false) @Size(max = 100) String sessionId,
			@RequestParam(required = false) @Min(1) @Max(50) Integer limit,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(
				"Homepage recommendations retrieved successfully",
				aiProxyService.getHomepageRecommendations(
						principal != null ? principal.getUserId() : null,
						sessionId,
						limit
				)
		);
	}

	@Operation(summary = "Get related product recommendations (Public)")
	@SecurityRequirements
	@GetMapping("/recommendations/product/{id}")
	public ApiResponse<AiRecommendationResponse> getRelatedProductRecommendations(
			@PathVariable @Positive Long id,
			@RequestParam(required = false) @Min(1) @Max(50) Integer limit
	) {
		return ApiResponse.success(
				"Related product recommendations retrieved successfully",
				aiProxyService.getRelatedRecommendations(id, limit)
		);
	}

	@Operation(summary = "Chat with AffiSmart AI assistant (Private)")
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/chat")
	public ApiResponse<AiChatResponse> chat(
			@Valid @RequestBody AiChatRequest request,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(
				"AI chat response retrieved successfully",
				aiProxyService.chat(principal != null ? principal.getUserId() : null, request)
		);
	}
}
