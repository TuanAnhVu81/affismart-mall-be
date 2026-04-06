package com.affismart.mall.modules.auth.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.auth.config.AuthProperties;
import com.affismart.mall.modules.auth.model.RefreshSessionEntry;
import com.affismart.mall.modules.auth.model.RefreshTokenSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RefreshTokenService {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

	private static final String SESSION_KEY_PREFIX = "auth:refresh:session:";
	private static final String LOOKUP_KEY_PREFIX = "auth:refresh:lookup:";
	private static final String USER_ZSET_KEY_PREFIX = "auth:refresh:user:";
	private static final String USED_TOKEN_KEY_PREFIX = "auth:refresh:used:";
	private static final String ROTATE_LOCK_KEY_PREFIX = "auth:refresh:lock:";

	private static final int MAX_IP_LENGTH = 64;
	private static final int MAX_USER_AGENT_LENGTH = 512;

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final Duration refreshTokenTtl;
	private final Duration usedTokenTtl;
	private final Duration rotateLockTtl;
	private final int maxSessions;
	private final int tokenEntropyBytes;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper,
			AuthProperties authProperties
	) {
		this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate, "stringRedisTemplate is required");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
		AuthProperties.RefreshToken properties = Objects.requireNonNull(
				authProperties.getRefreshToken(),
				"authProperties.refreshToken is required"
		);
		this.refreshTokenTtl = requirePositiveDuration(properties.getTtl(), "app.auth.refresh-token.ttl");
		this.usedTokenTtl = requirePositiveDuration(properties.getUsedTokenTtl(), "app.auth.refresh-token.used-token-ttl");
		this.rotateLockTtl = requirePositiveDuration(
				properties.getRotateLockTtl(),
				"app.auth.refresh-token.rotate-lock-ttl"
		);
		this.maxSessions = Math.max(properties.getMaxSessions(), 1);
		this.tokenEntropyBytes = Math.max(properties.getTokenEntropyBytes(), 32);
	}

	public RefreshTokenSession issue(Long userId) {
		return issue(userId, null, null);
	}

	public RefreshTokenSession issue(Long userId, String ipAddress, String userAgent) {
		long nowEpochMs = System.currentTimeMillis();
		String sessionId = UUID.randomUUID().toString();
		String token = generateRefreshToken();
		String tokenHash = hashToken(token);

		RefreshSessionEntry session = RefreshSessionEntry.active(
				sessionId,
				userId,
				tokenHash,
				sanitize(ipAddress, MAX_IP_LENGTH),
				sanitize(userAgent, MAX_USER_AGENT_LENGTH),
				nowEpochMs
		);
		storeSession(session);
		enforceSessionLimit(userId);
		return new RefreshTokenSession(token, userId, sessionId);
	}

	public RefreshTokenSession rotate(String refreshToken) {
		return rotate(refreshToken, null, null);
	}

	public RefreshTokenSession rotate(String refreshToken, String ipAddress, String userAgent) {
		String tokenHash = hashInputToken(refreshToken);
		RefreshSessionEntry currentSession = findSessionByTokenHash(tokenHash);
		if (currentSession == null) {
			handleInvalidOrReusedToken(tokenHash);
		}

		String rotateLockKey = buildRotateLockKey(currentSession.sessionId());
		Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
				rotateLockKey,
				tokenHash,
				rotateLockTtl
		);
		if (!Boolean.TRUE.equals(acquired)) {
			throw new AppException(ErrorCode.CONFLICT, "Refresh token rotation is already in progress");
		}

		try {
			RefreshSessionEntry latestSession = getSessionById(currentSession.sessionId());
			if (latestSession == null
					|| !RefreshSessionEntry.STATUS_ACTIVE.equals(latestSession.status())
					|| !tokenHash.equals(latestSession.currentTokenHash())) {
				handleInvalidOrReusedToken(tokenHash);
			}

			markTokenAsUsed(tokenHash, latestSession.userId());
			stringRedisTemplate.delete(buildLookupKey(tokenHash));

			String newToken = generateRefreshToken();
			String newTokenHash = hashToken(newToken);
			long nowEpochMs = System.currentTimeMillis();

			RefreshSessionEntry rotatedSession = latestSession.rotate(
					newTokenHash,
					sanitize(ipAddress, MAX_IP_LENGTH),
					sanitize(userAgent, MAX_USER_AGENT_LENGTH),
					nowEpochMs
			);
			storeSession(rotatedSession);
			return new RefreshTokenSession(newToken, rotatedSession.userId(), rotatedSession.sessionId());
		} finally {
			stringRedisTemplate.delete(rotateLockKey);
		}
	}

	public void revoke(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			return;
		}
		String tokenHash = hashToken(refreshToken);
		RefreshSessionEntry session = findSessionByTokenHash(tokenHash);
		if (session == null) {
			return;
		}
		revokeSession(session, false);
	}

	public void revokeAllSessions(Long userId) {
		if (userId == null) {
			return;
		}
		String userZSetKey = buildUserSessionZSetKey(userId);
		Set<String> sessionIds = stringRedisTemplate.opsForZSet().range(userZSetKey, 0, -1);
		if (sessionIds != null) {
			for (String sessionId : sessionIds) {
				revokeSessionById(sessionId, false);
			}
		}
		stringRedisTemplate.delete(userZSetKey);
	}

	public Long requireUserId(String refreshToken) {
		String tokenHash = hashInputToken(refreshToken);
		RefreshSessionEntry session = findSessionByTokenHash(tokenHash);
		if (session == null || !RefreshSessionEntry.STATUS_ACTIVE.equals(session.status())) {
			handleInvalidOrReusedToken(tokenHash);
		}
		return session.userId();
	}

	private void storeSession(RefreshSessionEntry session) {
		stringRedisTemplate.opsForValue().set(
				buildSessionKey(session.sessionId()),
				toJson(session),
				refreshTokenTtl
		);
		stringRedisTemplate.opsForValue().set(
				buildLookupKey(session.currentTokenHash()),
				session.sessionId(),
				refreshTokenTtl
		);
		stringRedisTemplate.opsForZSet().add(
				buildUserSessionZSetKey(session.userId()),
				session.sessionId(),
				session.updatedAtEpochMs()
		);
		stringRedisTemplate.expire(buildUserSessionZSetKey(session.userId()), refreshTokenTtl);
	}

	private void enforceSessionLimit(Long userId) {
		String userZSetKey = buildUserSessionZSetKey(userId);
		Long currentSessions = stringRedisTemplate.opsForZSet().zCard(userZSetKey);
		if (currentSessions == null || currentSessions <= maxSessions) {
			return;
		}

		long overflow = currentSessions - maxSessions;
		Set<String> oldestSessionIds = stringRedisTemplate.opsForZSet().range(userZSetKey, 0, overflow - 1);
		if (oldestSessionIds != null) {
			for (String sessionId : oldestSessionIds) {
				revokeSessionById(sessionId, false);
			}
		}
	}

	private void revokeSessionById(String sessionId, boolean markCurrentTokenUsed) {
		RefreshSessionEntry session = getSessionById(sessionId);
		if (session == null) {
			return;
		}
		revokeSession(session, markCurrentTokenUsed);
	}

	private void revokeSession(RefreshSessionEntry session, boolean markCurrentTokenUsed) {
		if (markCurrentTokenUsed) {
			markTokenAsUsed(session.currentTokenHash(), session.userId());
		}

		RefreshSessionEntry revokedSession = session.revoke(System.currentTimeMillis());
		stringRedisTemplate.opsForValue().set(
				buildSessionKey(revokedSession.sessionId()),
				toJson(revokedSession),
				refreshTokenTtl
		);
		stringRedisTemplate.delete(buildLookupKey(session.currentTokenHash()));
		stringRedisTemplate.opsForZSet().remove(buildUserSessionZSetKey(session.userId()), session.sessionId());
	}

	private void markTokenAsUsed(String tokenHash, Long userId) {
		stringRedisTemplate.opsForValue().set(
				buildUsedTokenKey(tokenHash),
				String.valueOf(userId),
				usedTokenTtl
		);
	}

	private RefreshSessionEntry findSessionByTokenHash(String tokenHash) {
		String sessionId = stringRedisTemplate.opsForValue().get(buildLookupKey(tokenHash));
		if (!StringUtils.hasText(sessionId)) {
			return null;
		}
		return getSessionById(sessionId);
	}

	private RefreshSessionEntry getSessionById(String sessionId) {
		String payload = stringRedisTemplate.opsForValue().get(buildSessionKey(sessionId));
		if (!StringUtils.hasText(payload)) {
			return null;
		}
		try {
			return objectMapper.readValue(payload, RefreshSessionEntry.class);
		} catch (JsonProcessingException exception) {
			log.warn("Invalid refresh session payload for session_id={}", sessionId);
			stringRedisTemplate.delete(buildSessionKey(sessionId));
			return null;
		}
	}

	private String toJson(RefreshSessionEntry session) {
		try {
			return objectMapper.writeValueAsString(session);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize refresh session payload", exception);
		}
	}

	private void handleInvalidOrReusedToken(String tokenHash) {
		if (!StringUtils.hasText(tokenHash)) {
			throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		String usedTokenValue = stringRedisTemplate.opsForValue().get(buildUsedTokenKey(tokenHash));
		if (StringUtils.hasText(usedTokenValue)) {
			try {
				Long userId = Long.valueOf(usedTokenValue);
				revokeAllSessions(userId);
			} catch (NumberFormatException exception) {
				log.warn("Invalid used-token bucket value for token_hash={}", tokenHash);
			}
			throw new AppException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
		}

		throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	private String hashInputToken(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		return hashToken(refreshToken);
	}

	private String hashToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return toHex(hashedBytes);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 digest is not available", exception);
		}
	}

	private String generateRefreshToken() {
		byte[] bytes = new byte[tokenEntropyBytes];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String toHex(byte[] bytes) {
		char[] hex = new char[bytes.length * 2];
		char[] chars = "0123456789abcdef".toCharArray();
		for (int i = 0; i < bytes.length; i++) {
			int value = bytes[i] & 0xFF;
			hex[i * 2] = chars[value >>> 4];
			hex[i * 2 + 1] = chars[value & 0x0F];
		}
		return new String(hex);
	}

	private Duration requirePositiveDuration(Duration value, String propertyName) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException(propertyName + " must be a positive duration");
		}
		return value;
	}

	private String sanitize(String value, int maxLength) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() <= maxLength) {
			return trimmed;
		}
		return trimmed.substring(0, maxLength);
	}

	private String buildSessionKey(String sessionId) {
		return SESSION_KEY_PREFIX + sessionId;
	}

	private String buildLookupKey(String tokenHash) {
		return LOOKUP_KEY_PREFIX + tokenHash;
	}

	private String buildUserSessionZSetKey(Long userId) {
		return USER_ZSET_KEY_PREFIX + userId + ":sessions";
	}

	private String buildUsedTokenKey(String tokenHash) {
		return USED_TOKEN_KEY_PREFIX + tokenHash;
	}

	private String buildRotateLockKey(String sessionId) {
		return ROTATE_LOCK_KEY_PREFIX + sessionId;
	}
}
