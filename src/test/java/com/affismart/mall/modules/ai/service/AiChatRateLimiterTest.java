package com.affismart.mall.modules.ai.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.ai.dto.request.AiChatRequest;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatRateLimiter Unit Tests")
class AiChatRateLimiterTest {

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	@DisplayName("checkAllowed: first request increments user key and sets 60s TTL")
	void checkAllowed_FirstUserRequest_IncrementsAndExpiresKey() {
		// Given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment("chat_ratelimit:12")).willReturn(1L);
		AiChatRateLimiter limiter = new AiChatRateLimiter(redisTemplate);

		// When
		limiter.checkAllowed(12L, new AiChatRequest("Hello", "browser-session"));

		// Then
		verify(valueOperations).increment("chat_ratelimit:12");
		verify(redisTemplate).expire("chat_ratelimit:12", Duration.ofSeconds(60));
	}

	@Test
	@DisplayName("checkAllowed: request within limit does not reset TTL")
	void checkAllowed_WithinLimit_DoesNotResetTtl() {
		// Given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment("chat_ratelimit:12")).willReturn(5L);
		AiChatRateLimiter limiter = new AiChatRateLimiter(redisTemplate);

		// When
		limiter.checkAllowed(12L, new AiChatRequest("Hello", null));

		// Then
		verify(redisTemplate, never()).expire("chat_ratelimit:12", Duration.ofSeconds(60));
	}

	@Test
	@DisplayName("checkAllowed: sixth request throws 429 AI_CHAT_RATE_LIMITED")
	void checkAllowed_ExceededLimit_ThrowsRateLimited() {
		// Given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment("chat_ratelimit:12")).willReturn(6L);
		AiChatRateLimiter limiter = new AiChatRateLimiter(redisTemplate);

		// When / Then
		assertThatThrownBy(() -> limiter.checkAllowed(12L, new AiChatRequest("Hello", null)))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AI_CHAT_RATE_LIMITED);

		verify(redisTemplate, never()).expire("chat_ratelimit:12", Duration.ofSeconds(60));
	}

	@Test
	@DisplayName("checkAllowed: anonymous fallback uses trimmed session id")
	void checkAllowed_NoUser_UsesTrimmedSessionId() {
		// Given
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment("chat_ratelimit:session-77")).willReturn(1L);
		AiChatRateLimiter limiter = new AiChatRateLimiter(redisTemplate);

		// When
		limiter.checkAllowed(null, new AiChatRequest("Hello", "  session-77  "));

		// Then
		verify(valueOperations).increment("chat_ratelimit:session-77");
		verify(redisTemplate).expire("chat_ratelimit:session-77", Duration.ofSeconds(60));
	}

	@Test
	@DisplayName("checkAllowed: Redis outage is fail-open so chat does not return 500")
	void checkAllowed_RedisUnavailable_AllowsRequest() {
		// Given
		given(redisTemplate.opsForValue()).willThrow(new RedisConnectionFailureException("Redis down"));
		AiChatRateLimiter limiter = new AiChatRateLimiter(redisTemplate);

		// When / Then
		limiter.checkAllowed(12L, new AiChatRequest("Hello", null));
	}
}
