package com.affismart.mall.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Enable JPA auditing to automatically populate @CreatedDate and @LastModifiedDate fields
@Configuration
@ConditionalOnBean(name = "jpaMappingContext")
@EnableJpaAuditing
public class JpaAuditingConfig {
}
