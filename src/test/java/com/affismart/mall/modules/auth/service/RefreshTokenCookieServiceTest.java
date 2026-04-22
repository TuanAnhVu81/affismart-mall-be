package com.affismart.mall.modules.auth.service;

import com.affismart.mall.modules.auth.config.AuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RefreshTokenCookieService Unit Tests")
class RefreshTokenCookieServiceTest {

	@Test
	@DisplayName("addRefreshTokenCookie: Happy Path - set-cookie header contains configured cookie attributes")
	void addRefreshTokenCookie_ValidToken_AddsConfiguredCookieHeader() {
		// Given
		RefreshTokenCookieService refreshTokenCookieService = new RefreshTokenCookieService(createAuthProperties());
		HttpHeaders headers = new HttpHeaders();

		// When
		refreshTokenCookieService.addRefreshTokenCookie(headers, "refresh-token-value");

		// Then
		assertThat(headers.get(HttpHeaders.SET_COOKIE)).hasSize(1);
		assertThat(headers.getFirst(HttpHeaders.SET_COOKIE))
				.contains("refresh_token=refresh-token-value")
				.contains("Path=/api/v1/auth")
				.contains("HttpOnly")
				.contains("Secure")
				.contains("SameSite=None");
	}

	@Test
	@DisplayName("clearRefreshTokenCookie: Happy Path - set-cookie header clears token with max-age zero")
	void clearRefreshTokenCookie_Always_AddsExpiredCookieHeader() {
		// Given
		RefreshTokenCookieService refreshTokenCookieService = new RefreshTokenCookieService(createAuthProperties());
		HttpHeaders headers = new HttpHeaders();

		// When
		refreshTokenCookieService.clearRefreshTokenCookie(headers);

		// Then
		assertThat(headers.get(HttpHeaders.SET_COOKIE)).hasSize(1);
		assertThat(headers.getFirst(HttpHeaders.SET_COOKIE))
				.contains("refresh_token=")
				.contains("Max-Age=0")
				.contains("Path=/api/v1/auth");
	}

	@Test
	@DisplayName("getCookieName: Happy Path - returns configured cookie name")
	void getCookieName_ConfiguredValue_ReturnsCookieName() {
		// Given
		RefreshTokenCookieService refreshTokenCookieService = new RefreshTokenCookieService(createAuthProperties());

		// When
		String result = refreshTokenCookieService.getCookieName();

		// Then
		assertThat(result).isEqualTo("refresh_token");
	}

	private AuthProperties createAuthProperties() {
		AuthProperties authProperties = new AuthProperties();
		authProperties.getRefreshToken().setCookieName("refresh_token");
		authProperties.getRefreshToken().setCookiePath("/api/v1/auth");
		authProperties.getRefreshToken().setSameSite("None");
		authProperties.getRefreshToken().setSecure(true);
		authProperties.getRefreshToken().setHttpOnly(true);
		return authProperties;
	}
}
