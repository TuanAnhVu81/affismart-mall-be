package com.affismart.mall.modules.product.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.common.response.PageResponse;
import com.affismart.mall.modules.product.dto.response.CategoryResponse;
import com.affismart.mall.modules.product.dto.response.ProductResponse;
import com.affismart.mall.modules.product.service.CategoryService;
import com.affismart.mall.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Catalog", description = "Admin endpoints for full catalog visibility")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

	private final CategoryService categoryService;
	private final ProductService productService;

	public AdminCatalogController(CategoryService categoryService, ProductService productService) {
		this.categoryService = categoryService;
		this.productService = productService;
	}

	@Operation(summary = "Get categories for admin, including inactive ones")
	@GetMapping("/categories")
	public ApiResponse<List<CategoryResponse>> getCategories(
			@RequestParam(required = false) Boolean active
	) {
		return ApiResponse.success(
				"Categories retrieved successfully",
				categoryService.getCategoriesForAdmin(active)
		);
	}

	@Operation(summary = "Get products for admin, including inactive ones")
	@GetMapping("/products")
	public ApiResponse<PageResponse<ProductResponse>> getProducts(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(name = "sort_by", defaultValue = "created_at") String sortBy,
			@RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir,
			@RequestParam(required = false) String keyword,
			@RequestParam(name = "category_id", required = false) Long categoryId,
			@RequestParam(name = "min_price", required = false) BigDecimal minPrice,
			@RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
			@RequestParam(required = false) Boolean active
	) {
		return ApiResponse.success(
				"Products retrieved successfully",
				productService.getAdminProducts(page, size, sortBy, sortDir, keyword, categoryId, minPrice, maxPrice, active)
		);
	}

	@Operation(summary = "Get product detail for admin by id, including inactive ones")
	@GetMapping("/products/{id}")
	public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
		return ApiResponse.success(
				"Product retrieved successfully",
				productService.getAdminProductById(id)
		);
	}
}
