package com.affismart.mall.modules.product.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.product.dto.request.UpdateCategoryStatusRequest;
import com.affismart.mall.modules.product.dto.request.UpsertCategoryRequest;
import com.affismart.mall.modules.product.dto.response.CategoryResponse;
import com.affismart.mall.modules.product.entity.Category;
import com.affismart.mall.modules.product.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Transactional
	public CategoryResponse createCategory(UpsertCategoryRequest request) {
		String normalizedName = normalizeName(request.name());
		String resolvedSlug = resolveSlug(request.slug(), normalizedName);
		ensureSlugUniqueForCreate(resolvedSlug);

		Category category = new Category();
		category.setName(normalizedName);
		category.setSlug(resolvedSlug);
		category.setActive(true);

		return toResponse(categoryRepository.save(category));
	}

	@Transactional
	public CategoryResponse updateCategory(Long categoryId, UpsertCategoryRequest request) {
		Category category = getRequiredCategory(categoryId);
		String normalizedName = normalizeName(request.name());
		String resolvedSlug = resolveSlug(request.slug(), normalizedName);
		ensureSlugUniqueForUpdate(resolvedSlug, categoryId);

		category.setName(normalizedName);
		category.setSlug(resolvedSlug);
		return toResponse(categoryRepository.save(category));
	}

	@Transactional
	public CategoryResponse updateCategoryStatus(Long categoryId, UpdateCategoryStatusRequest request) {
		Category category = getRequiredCategory(categoryId);
		category.setActive(request.active());
		return toResponse(categoryRepository.save(category));
	}

	private Category getRequiredCategory(Long categoryId) {
		return categoryRepository.findById(categoryId)
				.orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
	}

	private String normalizeName(String name) {
		return name == null ? null : name.trim();
	}

	private String resolveSlug(String slugInput, String fallbackName) {
		String rawSlug = StringUtils.hasText(slugInput) ? slugInput.trim() : fallbackName;
		String resolvedSlug = SlugUtils.toSlug(rawSlug);
		if (!StringUtils.hasText(resolvedSlug)) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Category slug is invalid");
		}
		return resolvedSlug;
	}

	private void ensureSlugUniqueForCreate(String slug) {
		if (categoryRepository.existsBySlugIgnoreCase(slug)) {
			throw new AppException(ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS);
		}
	}

	private void ensureSlugUniqueForUpdate(String slug, Long categoryId) {
		if (categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, categoryId)) {
			throw new AppException(ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS);
		}
	}

	private CategoryResponse toResponse(Category category) {
		return new CategoryResponse(
				category.getId(),
				category.getName(),
				category.getSlug(),
				category.isActive(),
				category.getCreatedAt(),
				category.getUpdatedAt()
		);
	}
}
