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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.affismart.mall.common.error.ErrorCode.INVALID_REFRESH_TOKEN;

@Tag(name = "Authentication", description = "Endpoints for user registration, login, and session management")
@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenCookieService refreshTokenCookieService;

	public AuthController(AuthService authService, RefreshTokenCookieService refreshTokenCookieService) {
		this.authService = authService;
		this.refreshTokenCookieService = refreshTokenCookieService;
	}

	@Operation(summary = "Register a new user account", description = "Creates a new user with default CUSTOMER role and ACTIVE status")
	@SecurityRequirements
	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthUserResponse>> register(@Valid @RequestBody RegisterRequest request) {
		AuthUserResponse response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Registration completed successfully", response));
	}

	@Operation(summary = "Login with email and password", description = "Returns an access token in the body and a refresh token in a secure HttpOnly cookie")
	@SecurityRequirements
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpServletRequest
	) {
		AuthenticatedSession session = authService.login(
				request,
				extractClientIp(httpServletRequest),
				extractUserAgent(httpServletRequest)
		);
		HttpHeaders headers = new HttpHeaders();
		refreshTokenCookieService.addRefreshTokenCookie(headers, session.refreshToken());
		return ResponseEntity.ok()
				.headers(headers)
				.body(ApiResponse.success("Login successful", session.tokenResponse()));
	}

	@Operation(summary = "Refresh access token", description = "Rotates the refresh token and returns a new access token")
	@SecurityRequirements
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(HttpServletRequest request) {
		String refreshToken = extractRefreshToken(request);
		AuthenticatedSession session = authService.refresh(
				refreshToken,
				extractClientIp(request),
				extractUserAgent(request)
		);
		HttpHeaders headers = new HttpHeaders();
		refreshTokenCookieService.addRefreshTokenCookie(headers, session.refreshToken());
		return ResponseEntity.ok()
				.headers(headers)
				.body(ApiResponse.success("Access token refreshed successfully", session.tokenResponse()));
	}

	@Operation(summary = "Logout user", description = "Revokes the refresh token and clears the secure cookie")
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

	private String extractClientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null) {
			int index = forwardedFor.indexOf(',');
			return index >= 0 ? forwardedFor.substring(0, index).trim() : forwardedFor.trim();
		}
		return request.getRemoteAddr();
	}

	private String extractUserAgent(HttpServletRequest request) {
		return request.getHeader("User-Agent");
	}
}
