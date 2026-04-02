package com.affismart.mall.modules.auth.service;

import com.affismart.mall.modules.auth.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private static final String USER_ID_CLAIM = "userId";
	private static final String ROLES_CLAIM = "roles";

	private final AuthProperties authProperties;
	private final SecretKey signingKey;

	public JwtService(AuthProperties authProperties) {
		this.authProperties = authProperties;
		this.signingKey = Keys.hmacShaKeyFor(
				authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
		);
	}

	public String generateAccessToken(Long userId, String subject, Collection<String> roles) {
		Instant now = Instant.now();
		Instant expiration = now.plus(authProperties.getJwt().getAccessTokenTtl());

		return Jwts.builder()
				.subject(subject)
				.issuer(authProperties.getJwt().getIssuer())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiration))
				.claim(USER_ID_CLAIM, userId)
				.claim(ROLES_CLAIM, List.copyOf(roles))
				.signWith(signingKey)
				.compact();
	}

	public boolean isTokenValid(String token) {
		try {
			parse(token);
			return true;
		} catch (JwtException | IllegalArgumentException exception) {
			return false;
		}
	}

	public String extractSubject(String token) {
		return parse(token).getPayload().getSubject();
	}

	public Long extractUserId(String token) {
		return parse(token).getPayload().get(USER_ID_CLAIM, Long.class);
	}

	public List<String> extractRoles(String token) {
		Object value = parse(token).getPayload().get(ROLES_CLAIM);
		if (value instanceof List<?> roles) {
			return roles.stream()
					.map(String::valueOf)
					.toList();
		}
		return List.of();
	}

	public Instant extractExpiration(String token) {
		return parse(token).getPayload().getExpiration().toInstant();
	}

	private Jws<Claims> parse(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token);
	}
}
