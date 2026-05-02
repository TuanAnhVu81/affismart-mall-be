package com.affismart.mall.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

	public static final String CATEGORIES_CACHE = "categories";
	public static final String ANALYTICS_DASHBOARD_CACHE = "analyticsDashboard";
	public static final String ANALYTICS_TOP_PRODUCTS_CACHE = "analyticsTopProducts";
	public static final String ANALYTICS_TOP_AFFILIATES_CACHE = "analyticsTopAffiliates";

	private static final Duration ANALYTICS_CACHE_TTL = Duration.ofSeconds(60);

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.setAllowNullValues(false);
		cacheManager.registerCustomCache(
				CATEGORIES_CACHE,
				Caffeine.newBuilder()
						.maximumSize(100)
						.build()
		);
		cacheManager.registerCustomCache(
				ANALYTICS_DASHBOARD_CACHE,
				analyticsCacheBuilder(10).build()
		);
		cacheManager.registerCustomCache(
				ANALYTICS_TOP_PRODUCTS_CACHE,
				analyticsCacheBuilder(100).build()
		);
		cacheManager.registerCustomCache(
				ANALYTICS_TOP_AFFILIATES_CACHE,
				analyticsCacheBuilder(100).build()
		);
		return cacheManager;
	}

	private Caffeine<Object, Object> analyticsCacheBuilder(long maximumSize) {
		return Caffeine.newBuilder()
				.expireAfterWrite(ANALYTICS_CACHE_TTL)
				.maximumSize(maximumSize);
	}
}
