package com.affismart.mall.modules.auth.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.auth.config.AuthProperties;
import com.affismart.mall.modules.auth.model.RefreshTokenSession;
import java.time.Duration;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RefreshTokenService {

	private static final String TOKEN_KEY_PREFIX = "auth:refresh:token:";
	private static final String USER_SET_KEY_PREFIX = "auth:refresh:user:";
	private static final String USED_TOKEN_KEY_PREFIX = "auth:refresh:used:";

	private final StringRedisTemplate stringRedisTemplate;
	private final Duration refreshTokenTtl;

	public RefreshTokenService(StringRedisTemplate stringRedisTemplate, AuthProperties authProperties) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.refreshTokenTtl = authProperties.getRefreshToken().getTtl();
	}

	public RefreshTokenSession issue(Long userId) {
		RefreshTokenSession session = RefreshTokenSession.issue(userId);
		store(session);
		return session;
	}

	public RefreshTokenSession rotate(String refreshToken) {
		Long userId = getActiveUserId(refreshToken);
		if (userId == null) {
			handleInvalidOrReusedToken(refreshToken);
		}

		revoke(refreshToken, true);
		return issue(userId);
	}

	public void revoke(String refreshToken) {
		revoke(refreshToken, false);
	}

	public void revokeAllSessions(Long userId) {
		String userSetKey = buildUserSetKey(userId);
		Set<String> activeTokens = stringRedisTemplate.opsForSet().members(userSetKey);
		if (activeTokens != null) {
			for (String token : activeTokens) {
				stringRedisTemplate.delete(buildTokenKey(token));
			}
		}
		stringRedisTemplate.delete(userSetKey);
	}

	public Long requireUserId(String refreshToken) {
		Long userId = getActiveUserId(refreshToken);
		if (userId == null) {
			handleInvalidOrReusedToken(refreshToken);
		}
		return userId;
	}

	private void store(RefreshTokenSession session) {
		String tokenKey = buildTokenKey(session.token());
		String userSetKey = buildUserSetKey(session.userId());
		String userId = String.valueOf(session.userId());

		stringRedisTemplate.opsForValue().set(tokenKey, userId, refreshTokenTtl);
		stringRedisTemplate.opsForSet().add(userSetKey, session.token());
		stringRedisTemplate.expire(userSetKey, refreshTokenTtl);
	}

	private void revoke(String refreshToken, boolean markAsUsed) {
		Long userId = getActiveUserId(refreshToken);
		if (userId == null) {
			return;
		}

		stringRedisTemplate.delete(buildTokenKey(refreshToken));
		stringRedisTemplate.opsForSet().remove(buildUserSetKey(userId), refreshToken);

		if (markAsUsed) {
			stringRedisTemplate.opsForValue().set(
					buildUsedTokenKey(refreshToken),
					String.valueOf(userId),
					refreshTokenTtl
			);
		}
	}

	private Long getActiveUserId(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			return null;
		}

		String value = stringRedisTemplate.opsForValue().get(buildTokenKey(refreshToken));
		return value == null ? null : Long.valueOf(value);
	}

	private void handleInvalidOrReusedToken(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		String usedTokenValue = stringRedisTemplate.opsForValue().get(buildUsedTokenKey(refreshToken));
		if (usedTokenValue != null) {
			revokeAllSessions(Long.valueOf(usedTokenValue));
			throw new AppException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
		}

		throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	private String buildTokenKey(String token) {
		return TOKEN_KEY_PREFIX + token;
	}

	private String buildUserSetKey(Long userId) {
		return USER_SET_KEY_PREFIX + userId + ":tokens";
	}

	private String buildUsedTokenKey(String token) {
		return USED_TOKEN_KEY_PREFIX + token;
	}
}
