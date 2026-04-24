package com.affismart.mall.modules.ai.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.modules.ai.dto.request.AiChatRequest;
import com.affismart.mall.modules.ai.dto.response.AiChatResponse;
import com.affismart.mall.modules.ai.dto.response.AiRecommendationResponse;
import com.affismart.mall.modules.ai.service.AiProxyService;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "Endpoints for AI recommendations and chatbot")
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

	private final AiProxyService aiProxyService;

	public AiController(AiProxyService aiProxyService) {
		this.aiProxyService = aiProxyService;
	}

	@Operation(summary = "Get homepage recommendations (Public, optional auth)")
	@GetMapping("/recommendations")
	public ApiResponse<AiRecommendationResponse> getHomepageRecommendations(
			@RequestParam(name = "session_id", required = false) String sessionId,
			@RequestParam(required = false) Integer limit,
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
	@GetMapping("/recommendations/product/{id}")
	public ApiResponse<AiRecommendationResponse> getRelatedProductRecommendations(
			@PathVariable Long id,
			@RequestParam(required = false) Integer limit
	) {
		return ApiResponse.success(
				"Related product recommendations retrieved successfully",
				aiProxyService.getRelatedRecommendations(id, limit)
		);
	}

	@Operation(summary = "Chat with AffiSmart AI assistant (Private)")
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
