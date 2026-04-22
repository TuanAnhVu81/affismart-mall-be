package com.affismart.mall.modules.product.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.modules.product.dto.request.UpdateCategoryStatusRequest;
import com.affismart.mall.modules.product.dto.request.UpsertCategoryRequest;
import com.affismart.mall.modules.product.dto.response.CategoryResponse;
import com.affismart.mall.modules.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Categories", description = "Endpoints for category management")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@Operation(summary = "Get active categories (Public)")
	@GetMapping
	public ApiResponse<List<CategoryResponse>> getCategories() {
		return ApiResponse.success("Categories retrieved successfully", categoryService.getActiveCategories());
	}

	@Operation(summary = "Get all categories for admin including inactive ones (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin")
	public ApiResponse<List<CategoryResponse>> getAdminCategories(
			@RequestParam(required = false) Boolean active
	) {
		return ApiResponse.success("Admin categories retrieved successfully", categoryService.getCategoriesForAdmin(active));
	}

	@Operation(summary = "Get active category by slug (Public)")
	@GetMapping("/{slug}")
	public ApiResponse<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
		return ApiResponse.success("Category retrieved successfully", categoryService.getActiveCategoryBySlug(slug));
	}

	@Operation(summary = "Create category (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody UpsertCategoryRequest request) {
		return ApiResponse.success("Category created successfully", categoryService.createCategory(request));
	}

	@Operation(summary = "Update category (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}")
	public ApiResponse<CategoryResponse> updateCategory(
			@PathVariable Long id,
			@Valid @RequestBody UpsertCategoryRequest request
	) {
		return ApiResponse.success("Category updated successfully", categoryService.updateCategory(id, request));
	}

	@Operation(summary = "Update category active status (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}/status")
	public ApiResponse<CategoryResponse> updateCategoryStatus(
			@PathVariable Long id,
			@Valid @RequestBody UpdateCategoryStatusRequest request
	) {
		return ApiResponse.success("Category status updated successfully", categoryService.updateCategoryStatus(id, request));
	}
}
