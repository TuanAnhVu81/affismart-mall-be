package com.affismart.mall.modules.user.service;

import com.affismart.mall.modules.user.config.AdminBootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrapService implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

	private final AdminBootstrapProperties properties;
	private final UserService userService;

	public AdminBootstrapService(AdminBootstrapProperties properties, UserService userService) {
		this.properties = properties;
		this.userService = userService;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.isEnabled()) {
			return;
		}

		validateProperties();
		userService.ensureAdminUser(
				properties.getEmail(),
				properties.getPassword(),
				properties.getFullName()
		);
		log.info("Admin bootstrap completed for email={}", properties.getEmail());
	}

	private void validateProperties() {
		if (!StringUtils.hasText(properties.getEmail())
				|| !StringUtils.hasText(properties.getPassword())
				|| !StringUtils.hasText(properties.getFullName())) {
			throw new IllegalStateException(
					"Admin bootstrap is enabled but ADMIN_EMAIL, ADMIN_PASSWORD, or ADMIN_FULL_NAME is missing"
			);
		}
	}
}
