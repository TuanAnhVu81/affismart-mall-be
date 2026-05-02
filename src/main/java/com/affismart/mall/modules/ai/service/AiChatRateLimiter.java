package com.affismart.mall.modules.ai.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.ai.dto.request.AiChatRequest;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiChatRateLimiter {

	private static final Logger log = LoggerFactory.getLogger(AiChatRateLimiter.class);
	private static final String KEY_PREFIX = "chat_ratelimit:";
	private static final Duration WINDOW = Duration.ofSeconds(60);
	private static final long MAX_REQUESTS_PER_WINDOW = 5L;

	private final RedisTemplate<String, String> redisTemplate;

	public AiChatRateLimiter(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void checkAllowed(Long userId, AiChatRequest request) {
		String key = buildKey(userId, request);
		try {
			Long currentCount = redisTemplate.opsForValue().increment(key);
			if (currentCount == null) {
				log.warn("AI chat rate limit increment returned null for key={}", key);
				return;
			}
			if (currentCount == 1L) {
				redisTemplate.expire(key, WINDOW);
			}
			if (currentCount > MAX_REQUESTS_PER_WINDOW) {
				throw new AppException(ErrorCode.AI_CHAT_RATE_LIMITED);
			}
		} catch (RedisConnectionFailureException exception) {
			log.warn("AI chat rate limiter skipped because Redis is unavailable: {}", exception.getMessage());
		}
	}

	private String buildKey(Long userId, AiChatRequest request) {
		if (userId != null) {
			return KEY_PREFIX + userId;
		}
		if (request != null && StringUtils.hasText(request.sessionId())) {
			return KEY_PREFIX + request.sessionId().trim();
		}
		return KEY_PREFIX + "anonymous";
	}
}
