package com.affismart.mall.modules.analytics.service;

import com.affismart.mall.config.CacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnalyticsService Cache Annotation Tests")
class AnalyticsServiceCachingTest {

	@Test
	@DisplayName("analytics read methods use short-lived cache names")
	void analyticsReadMethods_AreCacheable() throws NoSuchMethodException {
		assertCacheable("getDashboard", CacheConfig.ANALYTICS_DASHBOARD_CACHE);
		assertCacheable("getTopProducts", CacheConfig.ANALYTICS_TOP_PRODUCTS_CACHE, Integer.class);
		assertCacheable("getTopAffiliates", CacheConfig.ANALYTICS_TOP_AFFILIATES_CACHE, Integer.class);
	}

	private void assertCacheable(String methodName, String cacheName, Class<?>... parameterTypes)
			throws NoSuchMethodException {
		Cacheable cacheable = AnalyticsService.class
				.getMethod(methodName, parameterTypes)
				.getAnnotation(Cacheable.class);

		assertThat(cacheable).as(methodName + " cache annotation").isNotNull();
		assertThat(cacheable.cacheNames()).containsExactly(cacheName);
	}
}
