package com.affismart.mall.modules.product.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.integration.cloudinary.CloudinaryService;
import com.affismart.mall.modules.product.dto.response.ProductImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Products", description = "Endpoints for product and catalog management")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

	private final CloudinaryService cloudinaryService;

	public ProductController(CloudinaryService cloudinaryService) {
		this.cloudinaryService = cloudinaryService;
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
