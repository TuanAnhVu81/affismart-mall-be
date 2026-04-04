package com.affismart.mall.modules.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public class AdminBootstrapProperties {

	private boolean enabled;

	private String email;

	private String password;

	private String fullName = "AffiSmart Administrator";
}
