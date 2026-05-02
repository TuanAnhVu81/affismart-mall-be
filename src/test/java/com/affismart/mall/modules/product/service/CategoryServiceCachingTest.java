package com.affismart.mall.modules.product.service;

import com.affismart.mall.config.CacheConfig;
import com.affismart.mall.modules.product.dto.request.UpdateCategoryStatusRequest;
import com.affismart.mall.modules.product.dto.request.UpsertCategoryRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategoryService Cache Annotation Tests")
class CategoryServiceCachingTest {

	@Test
	@DisplayName("getActiveCategories: caches public category list")
	void getActiveCategories_IsCacheable() throws NoSuchMethodException {
		// When
		Cacheable cacheable = CategoryService.class
				.getMethod("getActiveCategories")
				.getAnnotation(Cacheable.class);

		// Then
		assertThat(cacheable).isNotNull();
		assertThat(cacheable.cacheNames()).containsExactly(CacheConfig.CATEGORIES_CACHE);
	}

	@Test
	@DisplayName("category mutations: evict category cache")
	void categoryMutations_EvictCategoryCache() throws NoSuchMethodException {
		assertEvictsCategoryCache("createCategory", UpsertCategoryRequest.class);
		assertEvictsCategoryCache("updateCategory", Long.class, UpsertCategoryRequest.class);
		assertEvictsCategoryCache("updateCategoryStatus", Long.class, UpdateCategoryStatusRequest.class);
	}

	private void assertEvictsCategoryCache(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		CacheEvict cacheEvict = CategoryService.class
				.getMethod(methodName, parameterTypes)
				.getAnnotation(CacheEvict.class);

		assertThat(cacheEvict).as(methodName + " cache eviction").isNotNull();
		assertThat(cacheEvict.cacheNames()).containsExactly(CacheConfig.CATEGORIES_CACHE);
		assertThat(cacheEvict.allEntries()).isTrue();
	}
}
