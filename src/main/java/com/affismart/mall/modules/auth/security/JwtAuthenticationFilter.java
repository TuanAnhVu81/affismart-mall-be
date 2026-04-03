package com.affismart.mall.modules.auth.security;

import com.affismart.mall.config.RestAuthenticationEntryPoint;
import com.affismart.mall.modules.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

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
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
		if (!StringUtils.hasText(token)) {
			handleAuthenticationFailure(request, response, new BadCredentialsException("Bearer token is missing"));
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
