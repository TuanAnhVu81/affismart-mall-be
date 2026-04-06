package com.affismart.mall.modules.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

	@NotNull
	private Jwt jwt = new Jwt();

	@NotNull
	private RefreshToken refreshToken = new RefreshToken();

	@Getter
	@Setter
	public static class Jwt {

		@NotBlank
		private String secret;

		@NotBlank
		private String issuer = "affismart-mall-be";

		@NotNull
		private Duration accessTokenTtl = Duration.ofMinutes(30);
	}

	@Getter
	@Setter
	public static class RefreshToken {

		@NotNull
		private Duration ttl = Duration.ofDays(7);

		@NotNull
		private Duration usedTokenTtl = Duration.ofDays(7);

		@NotNull
		private Duration rotateLockTtl = Duration.ofSeconds(5);

		@NotBlank
		private String cookieName = "refresh_token";

		@NotBlank
		private String cookiePath = "/api/v1/auth";

		@NotBlank
		private String sameSite = "Lax";

		private boolean secure;

		private boolean httpOnly = true;

		private int maxSessions = 5;

		private int tokenEntropyBytes = 32;
	}
}
