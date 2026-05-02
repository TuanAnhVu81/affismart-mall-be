package com.affismart.mall.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheConfig Unit Tests")
class CacheConfigTest {

	@Test
	@DisplayName("cacheManager: registers category and analytics caches")
	void cacheManager_RegistersExpectedCaches() {
		// Given
		CacheConfig cacheConfig = new CacheConfig();

		// When
		CacheManager cacheManager = cacheConfig.cacheManager();

		// Then
		assertThat(cacheManager.getCache(CacheConfig.CATEGORIES_CACHE)).isNotNull();
		assertThat(cacheManager.getCache(CacheConfig.ANALYTICS_DASHBOARD_CACHE)).isNotNull();
		assertThat(cacheManager.getCache(CacheConfig.ANALYTICS_TOP_PRODUCTS_CACHE)).isNotNull();
		assertThat(cacheManager.getCache(CacheConfig.ANALYTICS_TOP_AFFILIATES_CACHE)).isNotNull();
	}
}
