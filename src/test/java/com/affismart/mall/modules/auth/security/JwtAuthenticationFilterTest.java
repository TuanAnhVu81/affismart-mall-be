package com.affismart.mall.modules.auth.security;

import com.affismart.mall.config.RestAuthenticationEntryPoint;
import com.affismart.mall.modules.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private UserDetailsService userDetailsService;

	@Mock
	private RestAuthenticationEntryPoint authenticationEntryPoint;

	@Mock
	private FilterChain filterChain;

	@Captor
	private ArgumentCaptor<AuthenticationException> authenticationExceptionCaptor;

	@InjectMocks
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("shouldNotFilter: public product endpoint returns true")
	void shouldNotFilter_PublicProductEndpoint_ReturnsTrue() {
		// Given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/some-product");

		// When
		boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

		// Then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("shouldNotFilter: admin low-stock endpoint returns false")
	void shouldNotFilter_AdminLowStockEndpoint_ReturnsFalse() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/low-stock");

		boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("shouldNotFilter: admin category endpoint returns false")
	void shouldNotFilter_AdminCategoryEndpoint_ReturnsFalse() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/categories/admin");

		boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("shouldNotFilter: public category slug endpoint returns true")
	void shouldNotFilter_PublicCategorySlugEndpoint_ReturnsTrue() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/categories/electronics");

		boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("doFilterInternal: Edge Case - missing authorization header continues filter chain")
	void doFilterInternal_MissingAuthorizationHeader_ContinuesFilterChain() throws Exception {
		// Given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/me");
		MockHttpServletResponse response = new MockHttpServletResponse();

		// When
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtService, userDetailsService, authenticationEntryPoint);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("doFilterInternal: Exception Case - malformed authorization header triggers entry point")
	void doFilterInternal_MalformedAuthorizationHeader_CommencesUnauthorized() throws Exception {
		// Given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/me");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Token abc def");
		MockHttpServletResponse response = new MockHttpServletResponse();

		// When
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Then
		verify(authenticationEntryPoint).commence(eq(request), eq(response), authenticationExceptionCaptor.capture());
		assertThat(authenticationExceptionCaptor.getValue().getMessage())
				.contains("Authorization header format is invalid");
		verifyNoInteractions(jwtService, userDetailsService);
		verify(filterChain, never()).doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("doFilterInternal: Happy Path - raw token authenticates and sets security context")
	void doFilterInternal_ValidRawToken_SetsAuthenticationAndContinues() throws Exception {
		// Given
		String token = "raw-access-token";
		UserPrincipal principal = createPrincipal(15L, "user@example.com");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/me");
		request.addHeader(HttpHeaders.AUTHORIZATION, token);
		request.setRemoteAddr("203.0.113.9");
		MockHttpServletResponse response = new MockHttpServletResponse();

		given(jwtService.extractSubject(token)).willReturn("user@example.com");
		given(jwtService.extractUserId(token)).willReturn(15L);
		given(jwtService.isTokenValid(token)).willReturn(true);
		given(userDetailsService.loadUserByUsername("user@example.com")).willReturn(principal);

		// When
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Then
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user@example.com");
		verify(filterChain).doFilter(request, response);
		verify(authenticationEntryPoint, never()).commence(any(), any(), any());
	}

	@Test
	@DisplayName("doFilterInternal: Exception Case - user id mismatch clears context and triggers entry point")
	void doFilterInternal_UserIdMismatch_CommencesUnauthorized() throws Exception {
		// Given
		String token = "access-token";
		UserPrincipal principal = createPrincipal(21L, "user@example.com");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/me");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();

		given(jwtService.extractSubject(token)).willReturn("user@example.com");
		given(jwtService.extractUserId(token)).willReturn(22L);
		given(jwtService.isTokenValid(token)).willReturn(true);
		given(userDetailsService.loadUserByUsername("user@example.com")).willReturn(principal);

		// When
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Then
		verify(authenticationEntryPoint).commence(eq(request), eq(response), authenticationExceptionCaptor.capture());
		assertThat(authenticationExceptionCaptor.getValue().getMessage()).contains("Access token is invalid");
		verify(filterChain, never()).doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("doFilterInternal: Exception Case - jwt parsing failure triggers entry point")
	void doFilterInternal_JwtException_CommencesUnauthorized() throws Exception {
		// Given
		String token = "broken-token";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/me");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();

		given(jwtService.extractSubject(token)).willThrow(new JwtException("Token expired"));

		// When
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Then
		verify(authenticationEntryPoint).commence(eq(request), eq(response), authenticationExceptionCaptor.capture());
		assertThat(authenticationExceptionCaptor.getValue().getMessage()).contains("Access token is invalid");
		verify(filterChain, never()).doFilter(request, response);
	}

	@Test
	@DisplayName("doFilterInternal: Edge Case - existing authentication skips re-authentication")
	void doFilterInternal_ExistingAuthentication_SkipsJwtProcessing() throws Exception {
		// Given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/me");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ignored-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		SecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("existing-user", null)
		);

		// When
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtService, userDetailsService, authenticationEntryPoint);
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("existing-user");
	}

	private UserPrincipal createPrincipal(Long userId, String email) {
		return new UserPrincipal(
				userId,
				email,
				"hashed-password",
				java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
				true,
				true
		);
	}
}
