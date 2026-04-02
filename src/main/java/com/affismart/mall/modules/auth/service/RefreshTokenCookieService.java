package com.affismart.mall.modules.auth.service;

import com.affismart.mall.modules.auth.config.AuthProperties;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenCookieService {

	private final AuthProperties authProperties;

	public RefreshTokenCookieService(AuthProperties authProperties) {
		this.authProperties = authProperties;
	}

	public void addRefreshTokenCookie(HttpHeaders headers, String refreshToken) {
		headers.add(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(refreshToken, authProperties.getRefreshToken().getTtl()).toString());
	}

	public void clearRefreshTokenCookie(HttpHeaders headers) {
		headers.add(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie("", Duration.ZERO).toString());
	}

	public String getCookieName() {
		return authProperties.getRefreshToken().getCookieName();
	}

	private ResponseCookie buildRefreshTokenCookie(String value, Duration maxAge) {
		AuthProperties.RefreshToken properties = authProperties.getRefreshToken();
		return ResponseCookie.from(properties.getCookieName(), value)
				.httpOnly(properties.isHttpOnly())
				.secure(properties.isSecure())
				.sameSite(properties.getSameSite())
				.path(properties.getCookiePath())
				.maxAge(maxAge)
				.build();
	}
}
