package com.affismart.mall.modules.auth.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.auth.config.AuthProperties;
import com.affismart.mall.modules.auth.model.RefreshSessionEntry;
import com.affismart.mall.modules.auth.model.RefreshTokenSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private ZSetOperations<String, String> zSetOperations;

	@Captor
	private ArgumentCaptor<String> keyCaptor;

	@Captor
	private ArgumentCaptor<String> valueCaptor;

	@Captor
	private ArgumentCaptor<Duration> durationCaptor;

	private ObjectMapper objectMapper;
	private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		refreshTokenService = new RefreshTokenService(
				stringRedisTemplate,
				objectMapper,
				createAuthProperties(2)
		);
	}

	@Test
	@DisplayName("issue: Happy Path - stores session with sanitized metadata")
	void issue_ValidInput_StoresSessionWithSanitizedMetadata() throws Exception {
		// Given
		String longIpAddress = "  " + "1".repeat(80) + "  ";
		String longUserAgent = "  " + "A".repeat(530) + "  ";
		stubValueOperations();
		stubZSetOperations();
		given(zSetOperations.zCard(anyString())).willReturn(1L);

		// When
		RefreshTokenSession result = refreshTokenService.issue(7L, longIpAddress, longUserAgent);

		// Then
		assertThat(result.token()).isNotBlank();
		assertThat(result.userId()).isEqualTo(7L);
		assertThat(result.sessionId()).isNotBlank();

		verify(valueOperations, times(2)).set(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());
		List<String> capturedKeys = keyCaptor.getAllValues();
		List<String> capturedValues = valueCaptor.getAllValues();
		List<Duration> capturedDurations = durationCaptor.getAllValues();

		assertThat(capturedDurations).containsOnly(Duration.ofDays(7));
		assertThat(capturedKeys).anyMatch(key -> key.startsWith("auth:refresh:session:"));
		assertThat(capturedKeys).anyMatch(key -> key.startsWith("auth:refresh:lookup:"));

		String sessionPayload = capturedValues.get(capturedKeys.indexOf(
				capturedKeys.stream().filter(key -> key.startsWith("auth:refresh:session:")).findFirst().orElseThrow()
		));
		RefreshSessionEntry savedSession = objectMapper.readValue(sessionPayload, RefreshSessionEntry.class);
		assertThat(savedSession.userId()).isEqualTo(7L);
		assertThat(savedSession.status()).isEqualTo(RefreshSessionEntry.STATUS_ACTIVE);
		assertThat(savedSession.ipAddress()).hasSize(64);
		assertThat(savedSession.userAgent()).hasSize(512);

		verify(zSetOperations).add(eq("auth:refresh:user:7:sessions"), eq(result.sessionId()), anyDouble());
		verify(stringRedisTemplate).expire("auth:refresh:user:7:sessions", Duration.ofDays(7));
	}

	@Test
	@DisplayName("issue: Edge Case - session limit overflow revokes oldest session")
	void issue_SessionLimitExceeded_RevokesOldestSession() throws Exception {
		// Given
		refreshTokenService = new RefreshTokenService(
				stringRedisTemplate,
				objectMapper,
				createAuthProperties(1)
		);
		stubValueOperations();
		stubZSetOperations();
		given(zSetOperations.zCard("auth:refresh:user:5:sessions")).willReturn(2L);
		given(zSetOperations.range("auth:refresh:user:5:sessions", 0, 0)).willReturn(Set.of("old-session"));
		given(valueOperations.get("auth:refresh:session:old-session")).willReturn(
				objectMapper.writeValueAsString(createActiveSession(
						"old-session",
						5L,
						hashToken("old-refresh-token"),
						"198.51.100.10",
						"JUnit"
				))
		);

		// When
		refreshTokenService.issue(5L, "203.0.113.5", "JUnit");

		// Then
		verify(stringRedisTemplate).delete("auth:refresh:lookup:" + hashToken("old-refresh-token"));
		verify(zSetOperations).remove("auth:refresh:user:5:sessions", "old-session");
	}

	@Test
	@DisplayName("rotate: Exception Case - blank token throws INVALID_REFRESH_TOKEN")
	void rotate_BlankToken_ThrowsInvalidRefreshToken() {
		// When / Then
		assertThatThrownBy(() -> refreshTokenService.rotate("   "))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

		verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
	}

	@Test
	@DisplayName("rotate: Exception Case - unknown token throws INVALID_REFRESH_TOKEN")
	void rotate_UnknownToken_ThrowsInvalidRefreshToken() {
		// Given
		String refreshToken = "unknown-refresh-token";
		String tokenHash = hashToken(refreshToken);
		stubValueOperations();
		given(valueOperations.get("auth:refresh:lookup:" + tokenHash)).willReturn(null);
		given(valueOperations.get("auth:refresh:used:" + tokenHash)).willReturn(null);

		// When / Then
		assertThatThrownBy(() -> refreshTokenService.rotate(refreshToken))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("rotate: Exception Case - reused token revokes all sessions and throws reuse detected")
	void rotate_ReusedToken_RevokesAllSessionsAndThrowsReuseDetected() {
		// Given
		String refreshToken = "reused-refresh-token";
		String tokenHash = hashToken(refreshToken);
		stubValueOperations();
		stubZSetOperations();
		given(valueOperations.get("auth:refresh:lookup:" + tokenHash)).willReturn(null);
		given(valueOperations.get("auth:refresh:used:" + tokenHash)).willReturn("9");
		given(zSetOperations.range("auth:refresh:user:9:sessions", 0, -1)).willReturn(Set.of("session-1"));
		given(valueOperations.get("auth:refresh:session:session-1")).willReturn(
				writeJson(createActiveSession("session-1", 9L, hashToken("session-token"), "192.0.2.1", "JUnit"))
		);

		// When / Then
		assertThatThrownBy(() -> refreshTokenService.rotate(refreshToken))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);

		verify(stringRedisTemplate).delete("auth:refresh:lookup:" + hashToken("session-token"));
		verify(zSetOperations).remove("auth:refresh:user:9:sessions", "session-1");
		verify(stringRedisTemplate).delete("auth:refresh:user:9:sessions");
	}

	@Test
	@DisplayName("rotate: Exception Case - concurrent rotation lock throws CONFLICT")
	void rotate_RotationAlreadyInProgress_ThrowsConflict() {
		// Given
		String refreshToken = "current-refresh-token";
		String tokenHash = hashToken(refreshToken);
		RefreshSessionEntry session = createActiveSession("session-2", 11L, tokenHash, "203.0.113.20", "JUnit");

		stubValueOperations();
		given(valueOperations.get("auth:refresh:lookup:" + tokenHash)).willReturn("session-2");
		given(valueOperations.get("auth:refresh:session:session-2")).willReturn(writeJson(session));
		given(valueOperations.setIfAbsent("auth:refresh:lock:session-2", tokenHash, Duration.ofSeconds(5)))
				.willReturn(false);

		// When / Then
		assertThatThrownBy(() -> refreshTokenService.rotate(refreshToken))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CONFLICT);
	}

	@Test
	@DisplayName("rotate: Exception Case - invalid stored payload deletes corrupt session and throws invalid token")
	void rotate_InvalidStoredPayload_DeletesCorruptSessionAndThrowsInvalidToken() {
		// Given
		String refreshToken = "broken-refresh-token";
		String tokenHash = hashToken(refreshToken);
		stubValueOperations();
		given(valueOperations.get("auth:refresh:lookup:" + tokenHash)).willReturn("broken-session");
		given(valueOperations.get("auth:refresh:session:broken-session")).willReturn("{invalid-json");
		given(valueOperations.get("auth:refresh:used:" + tokenHash)).willReturn(null);

		// When / Then
		assertThatThrownBy(() -> refreshTokenService.rotate(refreshToken))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

		verify(stringRedisTemplate).delete("auth:refresh:session:broken-session");
	}

	@Test
	@DisplayName("requireUserId: Exception Case - revoked session throws INVALID_REFRESH_TOKEN")
	void requireUserId_RevokedSession_ThrowsInvalidRefreshToken() {
		// Given
		String refreshToken = "revoked-refresh-token";
		String tokenHash = hashToken(refreshToken);
		RefreshSessionEntry revokedSession = createActiveSession("session-3", 12L, tokenHash, null, null).revoke(
				System.currentTimeMillis()
		);
		stubValueOperations();
		given(valueOperations.get("auth:refresh:lookup:" + tokenHash)).willReturn("session-3");
		given(valueOperations.get("auth:refresh:session:session-3")).willReturn(writeJson(revokedSession));
		given(valueOperations.get("auth:refresh:used:" + tokenHash)).willReturn(null);

		// When / Then
		assertThatThrownBy(() -> refreshTokenService.requireUserId(refreshToken))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	private AuthProperties createAuthProperties(int maxSessions) {
		AuthProperties authProperties = new AuthProperties();
		authProperties.getRefreshToken().setTtl(Duration.ofDays(7));
		authProperties.getRefreshToken().setUsedTokenTtl(Duration.ofDays(3));
		authProperties.getRefreshToken().setRotateLockTtl(Duration.ofSeconds(5));
		authProperties.getRefreshToken().setMaxSessions(maxSessions);
		authProperties.getRefreshToken().setTokenEntropyBytes(32);
		return authProperties;
	}

	private RefreshSessionEntry createActiveSession(
			String sessionId,
			Long userId,
			String tokenHash,
			String ipAddress,
			String userAgent
	) {
		return RefreshSessionEntry.active(
				sessionId,
				userId,
				tokenHash,
				ipAddress,
				userAgent,
				System.currentTimeMillis()
		);
	}

	private String writeJson(RefreshSessionEntry session) {
		try {
			return objectMapper.writeValueAsString(session);
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashedBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			for (byte hashedByte : hashedBytes) {
				builder.append(String.format("%02x", hashedByte));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private void stubValueOperations() {
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
	}

	private void stubZSetOperations() {
		given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);
	}
}
