package com.affismart.mall.modules.auth.security;

import com.affismart.mall.config.RestAuthenticationEntryPoint;
import com.affismart.mall.modules.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
	private static final Set<String> RESERVED_PRODUCT_GET_PATHS = Set.of("search", "low-stock", "admin");
	private static final Set<String> RESERVED_CATEGORY_GET_PATHS = Set.of("admin");

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;

	public JwtAuthenticationFilter(
			JwtService jwtService,
			UserDetailsService userDetailsService,
			RestAuthenticationEntryPoint authenticationEntryPoint
	) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String method = request.getMethod();

		return matches(method, requestUri, "POST", "/api/v1/auth/register")
				|| matches(method, requestUri, "POST", "/api/v1/auth/login")
				|| matches(method, requestUri, "POST", "/api/v1/auth/refresh")
				|| matches(method, requestUri, "POST", "/api/v1/payment/webhook")
				|| matches(method, requestUri, "GET", "/api/v1/payment/success")
				|| matches(method, requestUri, "GET", "/api/v1/payment/cancel")
				|| isHealthCheckRequest(method, requestUri)
				|| PATH_MATCHER.match("/swagger-ui/**", requestUri)
				|| "/swagger-ui.html".equals(requestUri)
				|| PATH_MATCHER.match("/v3/api-docs/**", requestUri)
				|| "/error".equals(requestUri)
				|| isPublicProductGetRequest(method, requestUri)
				|| isPublicCategoryGetRequest(method, requestUri);
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (!StringUtils.hasText(authorizationHeader)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = extractToken(authorizationHeader);
		if (!StringUtils.hasText(token)) {
			handleAuthenticationFailure(
					request,
					response,
					new BadCredentialsException("Authorization header format is invalid")
			);
			return;
		}

		try {
			if (SecurityContextHolder.getContext().getAuthentication() == null) {
				String subject = jwtService.extractSubject(token);
				Long userId = jwtService.extractUserId(token);
				UserDetails userDetails = userDetailsService.loadUserByUsername(subject);

				if (!jwtService.isTokenValid(token)
						|| !subject.equals(userDetails.getUsername())
						|| !matchesUserId(userDetails, userId)) {
					throw new BadCredentialsException("Access token is invalid");
				}

				UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
						userDetails,
						null,
						userDetails.getAuthorities()
				);
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}

			filterChain.doFilter(request, response);
		} catch (AuthenticationException exception) {
			handleAuthenticationFailure(request, response, exception);
		} catch (JwtException | IllegalArgumentException exception) {
			handleAuthenticationFailure(
					request,
					response,
					new BadCredentialsException("Access token is invalid", exception)
			);
		}
	}

	private boolean matchesUserId(UserDetails userDetails, Long userId) {
		return userDetails instanceof UserPrincipal userPrincipal
				&& Objects.equals(userPrincipal.getUserId(), userId);
	}

	private boolean matches(String method, String requestUri, String expectedMethod, String pattern) {
		return expectedMethod.equalsIgnoreCase(method) && PATH_MATCHER.match(pattern, requestUri);
	}

	private boolean isHealthCheckRequest(String method, String requestUri) {
		return ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method))
				&& PATH_MATCHER.match("/api/v1/health/**", requestUri);
	}

	private boolean isPublicProductGetRequest(String method, String requestUri) {
		if (!"GET".equalsIgnoreCase(method)) {
			return false;
		}

		if ("/api/v1/products".equals(requestUri) || "/api/v1/products/search".equals(requestUri)) {
			return true;
		}

		String slug = extractSingleSegment("/api/v1/products/", requestUri);
		return slug != null && !RESERVED_PRODUCT_GET_PATHS.contains(slug);
	}

	private boolean isPublicCategoryGetRequest(String method, String requestUri) {
		if (!"GET".equalsIgnoreCase(method)) {
			return false;
		}

		if ("/api/v1/categories".equals(requestUri)) {
			return true;
		}

		String slug = extractSingleSegment("/api/v1/categories/", requestUri);
		return slug != null && !RESERVED_CATEGORY_GET_PATHS.contains(slug);
	}

	private String extractSingleSegment(String prefix, String requestUri) {
		if (!requestUri.startsWith(prefix) || requestUri.length() <= prefix.length()) {
			return null;
		}

		String remainder = requestUri.substring(prefix.length());
		return remainder.contains("/") ? null : remainder;
	}

	private String extractToken(String authorizationHeader) {
		String value = authorizationHeader.trim();
		if (!StringUtils.hasText(value)) {
			return null;
		}

		if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			String bearerToken = value.substring(BEARER_PREFIX.length()).trim();
			return StringUtils.hasText(bearerToken) ? bearerToken : null;
		}

		// Support raw token value for clients that accidentally omit "Bearer ".
		return value.contains(" ") ? null : value;
	}

	private void handleAuthenticationFailure(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception
	) throws IOException, ServletException {
		SecurityContextHolder.clearContext();
		log.warn(
				"JWT authentication failed: method={}, uri={}, remoteAddr={}, reason={}",
				request.getMethod(),
				request.getRequestURI(),
				request.getRemoteAddr(),
				exception.getMessage()
		);
		authenticationEntryPoint.commence(request, response, exception);
	}
}
