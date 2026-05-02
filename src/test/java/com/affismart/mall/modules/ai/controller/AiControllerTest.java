package com.affismart.mall.modules.ai.controller;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.ai.dto.request.AiChatRequest;
import com.affismart.mall.modules.ai.dto.response.AiChatResponse;
import com.affismart.mall.modules.ai.service.AiChatRateLimiter;
import com.affismart.mall.modules.ai.service.AiProxyService;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiController Unit Tests")
class AiControllerTest {

	@Mock
	private AiProxyService aiProxyService;

	@Mock
	private AiChatRateLimiter aiChatRateLimiter;

	@Test
	@DisplayName("chat: checks rate limit before proxying to FastAPI")
	void chat_ValidRequest_ChecksRateLimitBeforeProxy() {
		// Given
		AiController controller = new AiController(aiProxyService, aiChatRateLimiter);
		AiChatRequest request = new AiChatRequest("Hello", "session-77");
		UserPrincipal principal = createPrincipal(12L);
		AiChatResponse aiResponse = new AiChatResponse(
				"Hello from AI",
				false,
				"gemini-test",
				OffsetDateTime.now()
		);
		given(aiProxyService.chat(12L, request)).willReturn(aiResponse);

		// When
		var response = controller.chat(request, principal);

		// Then
		assertThat(response.data()).isEqualTo(aiResponse);
		InOrder inOrder = inOrder(aiChatRateLimiter, aiProxyService);
		inOrder.verify(aiChatRateLimiter).checkAllowed(12L, request);
		inOrder.verify(aiProxyService).chat(12L, request);
	}

	@Test
	@DisplayName("chat: rate limited request does not call FastAPI proxy")
	void chat_RateLimited_DoesNotCallProxy() {
		// Given
		AiController controller = new AiController(aiProxyService, aiChatRateLimiter);
		AiChatRequest request = new AiChatRequest("Hello", "session-77");
		UserPrincipal principal = createPrincipal(12L);
		willThrow(new AppException(ErrorCode.AI_CHAT_RATE_LIMITED))
				.given(aiChatRateLimiter)
				.checkAllowed(12L, request);

		// When / Then
		assertThatThrownBy(() -> controller.chat(request, principal))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AI_CHAT_RATE_LIMITED);

		verify(aiProxyService, never()).chat(12L, request);
	}

	private UserPrincipal createPrincipal(Long userId) {
		return new UserPrincipal(
				userId,
				"user@example.com",
				"password",
				List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
				true,
				true
		);
	}
}
