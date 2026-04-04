package com.affismart.mall.config;

import com.affismart.mall.modules.user.config.AdminBootstrapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class BootstrapConfig {
}
