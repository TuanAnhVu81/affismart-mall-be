package com.affismart.mall.modules.auth.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.auth.dto.request.LoginRequest;
import com.affismart.mall.modules.auth.dto.request.RegisterRequest;
import com.affismart.mall.modules.auth.dto.response.AuthTokenResponse;
import com.affismart.mall.modules.auth.dto.response.AuthUserResponse;
import com.affismart.mall.modules.auth.model.AuthenticatedSession;
import com.affismart.mall.modules.auth.service.AuthService;
import com.affismart.mall.modules.auth.service.RefreshTokenCookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.affismart.mall.common.error.ErrorCode.INVALID_REFRESH_TOKEN;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenCookieService refreshTokenCookieService;

	public AuthController(AuthService authService, RefreshTokenCookieService refreshTokenCookieService) {
		this.authService = authService;
		this.refreshTokenCookieService = refreshTokenCookieService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthUserResponse>> register(@Valid @RequestBody RegisterRequest request) {
		AuthUserResponse response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Registration completed successfully", response));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request) {
		AuthenticatedSession session = authService.login(request);
		HttpHeaders headers = new HttpHeaders();
		refreshTokenCookieService.addRefreshTokenCookie(headers, session.refreshToken());
		return ResponseEntity.ok()
				.headers(headers)
				.body(ApiResponse.success("Login successful", session.tokenResponse()));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(HttpServletRequest request) {
		String refreshToken = extractRefreshToken(request);
		AuthenticatedSession session = authService.refresh(refreshToken);
		HttpHeaders headers = new HttpHeaders();
		refreshTokenCookieService.addRefreshTokenCookie(headers, session.refreshToken());
		return ResponseEntity.ok()
				.headers(headers)
				.body(ApiResponse.success("Access token refreshed successfully", session.tokenResponse()));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
		String refreshToken = extractOptionalRefreshToken(request);
		authService.logout(refreshToken);
		HttpHeaders headers = new HttpHeaders();
		refreshTokenCookieService.clearRefreshTokenCookie(headers);
		return ResponseEntity.ok()
				.headers(headers)
				.body(ApiResponse.success("Logout successful"));
	}

	private String extractRefreshToken(HttpServletRequest request) {
		String refreshToken = extractOptionalRefreshToken(request);
		if (refreshToken == null) {
			throw new AppException(INVALID_REFRESH_TOKEN);
		}
		return refreshToken;
	}

	private String extractOptionalRefreshToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		String cookieName = refreshTokenCookieService.getCookieName();
		for (Cookie cookie : cookies) {
			if (cookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
