package com.affismart.mall.modules.auth.service;

import com.affismart.mall.modules.auth.config.AuthProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

	@Test
	@DisplayName("generateAccessToken: Happy Path - token contains expected subject and claims")
	void generateAccessToken_ValidInput_ReturnsTokenWithExpectedClaims() {
		// Given
		AuthProperties authProperties = createAuthProperties();
		JwtService jwtService = new JwtService(authProperties);

		// When
		String token = jwtService.generateAccessToken(7L, "user@gmail.com", List.of("CUSTOMER", "AFFILIATE"));

		// Then
		assertThat(token).isNotBlank();
		assertThat(jwtService.isTokenValid(token)).isTrue();
		assertThat(jwtService.extractSubject(token)).isEqualTo("user@gmail.com");
		assertThat(jwtService.extractUserId(token)).isEqualTo(7L);
		assertThat(jwtService.extractRoles(token)).containsExactly("CUSTOMER", "AFFILIATE");
		assertThat(jwtService.extractExpiration(token)).isAfter(Instant.now());
	}

	@Test
	@DisplayName("isTokenValid: Exception Case - malformed token returns false")
	void isTokenValid_MalformedToken_ReturnsFalse() {
		// Given
		JwtService jwtService = new JwtService(createAuthProperties());

		// When
		boolean result = jwtService.isTokenValid("not-a-jwt-token");

		// Then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("extractRoles: Edge Case - token without roles claim returns empty list")
	void extractRoles_TokenWithoutRolesClaim_ReturnsEmptyList() {
		// Given
		AuthProperties authProperties = createAuthProperties();
		JwtService jwtService = new JwtService(authProperties);
		String token = createTokenWithoutRolesClaim(authProperties, 9L, "norole@example.com");

		// When
		List<String> result = jwtService.extractRoles(token);

		// Then
		assertThat(result).isEmpty();
	}

	private AuthProperties createAuthProperties() {
		AuthProperties authProperties = new AuthProperties();
		authProperties.getJwt().setSecret("0123456789012345678901234567890123456789012345678901234567890123");
		authProperties.getJwt().setIssuer("affismart-mall-be");
		authProperties.getJwt().setAccessTokenTtl(Duration.ofMinutes(30));
		return authProperties;
	}

	private String createTokenWithoutRolesClaim(AuthProperties authProperties, Long userId, String subject) {
		SecretKey signingKey = Keys.hmacShaKeyFor(
				authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
		);
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(subject)
				.issuer(authProperties.getJwt().getIssuer())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(authProperties.getJwt().getAccessTokenTtl())))
				.claim("userId", userId)
				.signWith(signingKey)
				.compact();
	}
}
