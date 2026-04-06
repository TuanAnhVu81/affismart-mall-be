package com.affismart.mall.modules.product.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.integration.cloudinary.CloudinaryService;
import com.affismart.mall.modules.product.dto.request.UpdateProductStatusRequest;
import com.affismart.mall.modules.product.dto.request.UpsertProductRequest;
import com.affismart.mall.modules.product.dto.response.ProductImageUploadResponse;
import com.affismart.mall.modules.product.dto.response.ProductResponse;
import com.affismart.mall.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Products", description = "Endpoints for product and catalog management")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

	private final CloudinaryService cloudinaryService;
	private final ProductService productService;

	public ProductController(CloudinaryService cloudinaryService, ProductService productService) {
		this.cloudinaryService = cloudinaryService;
		this.productService = productService;
	}

	@Operation(summary = "Create product (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody UpsertProductRequest request) {
		return ApiResponse.success("Product created successfully", productService.createProduct(request));
	}

	@Operation(summary = "Update product (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}")
	public ApiResponse<ProductResponse> updateProduct(
			@PathVariable Long id,
			@Valid @RequestBody UpsertProductRequest request
	) {
		return ApiResponse.success("Product updated successfully", productService.updateProduct(id, request));
	}

	@Operation(summary = "Update product active status (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}/status")
	public ApiResponse<ProductResponse> updateProductStatus(
			@PathVariable Long id,
			@Valid @RequestBody UpdateProductStatusRequest request
	) {
		return ApiResponse.success("Product status updated successfully", productService.updateProductStatus(id, request));
	}

	@Operation(summary = "Upload product image (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<ProductImageUploadResponse> uploadProductImage(@RequestPart("file") MultipartFile file) {
		String secureUrl = cloudinaryService.uploadProductImage(file);
		return ApiResponse.success(
				"Product image uploaded successfully",
				new ProductImageUploadResponse(secureUrl)
		);
	}
}
