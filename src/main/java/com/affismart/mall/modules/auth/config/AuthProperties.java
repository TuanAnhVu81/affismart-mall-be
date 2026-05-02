package com.affismart.mall.modules.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
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
		@DurationMin(minutes = 5)
		@DurationMax(hours = 24)
		private Duration accessTokenTtl = Duration.ofMinutes(30);
	}

	@Getter
	@Setter
	public static class RefreshToken {

		@NotNull
		@DurationMin(days = 1)
		private Duration ttl = Duration.ofDays(7);

		@NotNull
		@DurationMin(hours = 1)
		private Duration usedTokenTtl = Duration.ofDays(7);

		@NotNull
		@DurationMin(seconds = 1)
		@DurationMax(minutes = 1)
		private Duration rotateLockTtl = Duration.ofSeconds(5);

		@NotBlank
		private String cookieName = "refresh_token";

		@NotBlank
		private String cookiePath = "/api/v1/auth";

		@NotBlank
		private String sameSite = "Lax";

		private boolean secure;

		private boolean httpOnly = true;

		@Min(1)
		private int maxSessions = 5;

		@Min(32)
		private int tokenEntropyBytes = 32;
	}
}
