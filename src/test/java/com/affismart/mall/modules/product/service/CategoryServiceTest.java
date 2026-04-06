package com.affismart.mall.modules.product.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.product.dto.request.UpdateCategoryStatusRequest;
import com.affismart.mall.modules.product.dto.request.UpsertCategoryRequest;
import com.affismart.mall.modules.product.dto.response.CategoryResponse;
import com.affismart.mall.modules.product.entity.Category;
import com.affismart.mall.modules.product.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private CategoryService categoryService;

	@Captor
	private ArgumentCaptor<Category> categoryCaptor;

	// =========================================================
	// createCategory()
	// =========================================================

	@Test
	@DisplayName("createCategory: Happy Path - slug is auto-generated from name when slug is blank")
	void createCategory_BlankSlug_GeneratesSlugFromName() {
		// Given
		UpsertCategoryRequest request = new UpsertCategoryRequest(" Dien Thoai Cao Cap ", " ");
		Category savedCategory = createMockCategory(10L, "Dien Thoai Cao Cap", "dien-thoai-cao-cap", true);

		given(categoryRepository.existsBySlugIgnoreCase("dien-thoai-cao-cap")).willReturn(false);
		given(categoryRepository.save(any(Category.class))).willReturn(savedCategory);

		// When
		CategoryResponse result = categoryService.createCategory(request);

		// Then - verify name is trimmed and slug is auto-generated
		verify(categoryRepository).save(categoryCaptor.capture());
		assertThat(categoryCaptor.getValue().getSlug()).isEqualTo("dien-thoai-cao-cap");
		assertThat(categoryCaptor.getValue().getName()).isEqualTo("Dien Thoai Cao Cap");
		assertThat(categoryCaptor.getValue().isActive()).isTrue();
		assertThat(result.slug()).isEqualTo("dien-thoai-cao-cap");
	}

	@Test
	@DisplayName("createCategory: Happy Path - explicit slug is used when provided")
	void createCategory_ExplicitSlug_UsesProvidedSlug() {
		// Given
		UpsertCategoryRequest request = new UpsertCategoryRequest("Laptop Gaming", "laptop-gaming-2024");
		Category savedCategory = createMockCategory(11L, "Laptop Gaming", "laptop-gaming-2024", true);

		given(categoryRepository.existsBySlugIgnoreCase("laptop-gaming-2024")).willReturn(false);
		given(categoryRepository.save(any(Category.class))).willReturn(savedCategory);

		// When
		CategoryResponse result = categoryService.createCategory(request);

		// Then - verify explicit slug is stored as-is
		verify(categoryRepository).save(categoryCaptor.capture());
		assertThat(categoryCaptor.getValue().getSlug()).isEqualTo("laptop-gaming-2024");
		assertThat(result.id()).isEqualTo(11L);
	}

	@Test
	@DisplayName("createCategory: Exception - slug already taken throws CATEGORY_SLUG_ALREADY_EXISTS")
	void createCategory_SlugAlreadyExists_ThrowsConflict() {
		// Given
		UpsertCategoryRequest request = new UpsertCategoryRequest("Electronics", "electronics");

		given(categoryRepository.existsBySlugIgnoreCase("electronics")).willReturn(true);

		// When + Then
		assertThatThrownBy(() -> categoryService.createCategory(request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS);

		// Verify nothing was saved when slug conflicts
		verify(categoryRepository, never()).save(any());
	}

	// =========================================================
	// updateCategory()
	// =========================================================

	@Test
	@DisplayName("updateCategory: Happy Path - name and slug are updated successfully")
	void updateCategory_ValidRequest_UpdatesNameAndSlug() {
		// Given
		Long categoryId = 5L;
		Category existing = createMockCategory(categoryId, "Old Name", "old-name", true);
		UpsertCategoryRequest request = new UpsertCategoryRequest("New Name", "new-name");
		Category savedCategory = createMockCategory(categoryId, "New Name", "new-name", true);

		given(categoryRepository.findById(categoryId)).willReturn(Optional.of(existing));
		given(categoryRepository.existsBySlugIgnoreCaseAndIdNot("new-name", categoryId)).willReturn(false);
		given(categoryRepository.save(any(Category.class))).willReturn(savedCategory);

		// When
		CategoryResponse result = categoryService.updateCategory(categoryId, request);

		// Then - verify the entity was updated with new values before saving
		verify(categoryRepository).save(categoryCaptor.capture());
		assertThat(categoryCaptor.getValue().getName()).isEqualTo("New Name");
		assertThat(categoryCaptor.getValue().getSlug()).isEqualTo("new-name");
		assertThat(result.name()).isEqualTo("New Name");
	}

	@Test
	@DisplayName("updateCategory: Exception - duplicate slug throws CATEGORY_SLUG_ALREADY_EXISTS")
	void updateCategory_DuplicateSlug_ThrowsConflict() {
		// Given
		Long categoryId = 5L;
		Category existing = createMockCategory(categoryId, "Old Name", "old-name", true);
		UpsertCategoryRequest request = new UpsertCategoryRequest("New Name", "shared-slug");

		given(categoryRepository.findById(categoryId)).willReturn(Optional.of(existing));
		given(categoryRepository.existsBySlugIgnoreCaseAndIdNot("shared-slug", categoryId)).willReturn(true);

		// When + Then
		assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CATEGORY_SLUG_ALREADY_EXISTS);

		verify(categoryRepository, never()).save(any());
	}

	@Test
	@DisplayName("updateCategory: Exception - category not found throws CATEGORY_NOT_FOUND")
	void updateCategory_NotFound_ThrowsCategoryNotFound() {
		// Given
		Long categoryId = 99L;
		UpsertCategoryRequest request = new UpsertCategoryRequest("Any Name", null);

		given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

		// When + Then
		assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
	}

	// =========================================================
	// updateCategoryStatus()
	// =========================================================

	@Test
	@DisplayName("updateCategoryStatus: Happy Path - category is deactivated (soft delete)")
	void updateCategoryStatus_SetInactive_PersistsChange() {
		// Given
		Long categoryId = 3L;
		Category existingActive = createMockCategory(categoryId, "Electronics", "electronics", true);
		UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest(false);
		Category savedCategory = createMockCategory(categoryId, "Electronics", "electronics", false);

		given(categoryRepository.findById(categoryId)).willReturn(Optional.of(existingActive));
		given(categoryRepository.save(any(Category.class))).willReturn(savedCategory);

		// When
		CategoryResponse result = categoryService.updateCategoryStatus(categoryId, request);

		// Then - verify is_active was toggled to false (soft delete)
		verify(categoryRepository).save(categoryCaptor.capture());
		assertThat(categoryCaptor.getValue().isActive()).isFalse();
		assertThat(result.active()).isFalse();
	}

	@Test
	@DisplayName("updateCategoryStatus: Exception - category not found throws CATEGORY_NOT_FOUND")
	void updateCategoryStatus_NotFound_ThrowsCategoryNotFound() {
		// Given
		UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest(false);
		given(categoryRepository.findById(99L)).willReturn(Optional.empty());

		// When + Then
		assertThatThrownBy(() -> categoryService.updateCategoryStatus(99L, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
	}

	// =========================================================
	// Public storefront methods
	// =========================================================

	@Test
	@DisplayName("getActiveCategories: Happy Path - returns only active categories ordered by name")
	void getActiveCategories_ReturnsMappedActiveList() {
		// Given
		given(categoryRepository.findAllByActiveTrueOrderByNameAsc()).willReturn(List.of(
				createMockCategory(1L, "Electronics", "electronics", true),
				createMockCategory(2L, "Fashion", "fashion", true)
		));

		// When
		List<CategoryResponse> result = categoryService.getActiveCategories();

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).name()).isEqualTo("Electronics");
		assertThat(result.get(1).slug()).isEqualTo("fashion");
	}

	@Test
	@DisplayName("getActiveCategoryById: Happy Path - returns category response when found and is active")
	void getActiveCategoryById_Found_ReturnsMappedResponse() {
		// Given
		Category found = createMockCategory(5L, "Electronics", "electronics", true);
		given(categoryRepository.findByIdAndActiveTrue(5L)).willReturn(Optional.of(found));

		// When
		CategoryResponse result = categoryService.getActiveCategoryById(5L);

		// Then
		assertThat(result.id()).isEqualTo(5L);
		assertThat(result.name()).isEqualTo("Electronics");
		assertThat(result.slug()).isEqualTo("electronics");
		assertThat(result.active()).isTrue();
	}

	@Test
	@DisplayName("getActiveCategoryById: Exception - inactive or missing category throws CATEGORY_NOT_FOUND")
	void getActiveCategoryById_NotFound_ThrowsCategoryNotFound() {
		// Given
		given(categoryRepository.findByIdAndActiveTrue(99L)).willReturn(Optional.empty());

		// When + Then
		assertThatThrownBy(() -> categoryService.getActiveCategoryById(99L))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
	}

	// =========================================================
	// Private Helper Methods
	// =========================================================

	private Category createMockCategory(Long id, String name, String slug, boolean active) {
		Category category = new Category();
		category.setId(id);
		category.setName(name);
		category.setSlug(slug);
		category.setActive(active);
		return category;
	}
}
