package com.affismart.mall.modules.product.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.product.dto.request.UpdateProductStatusRequest;
import com.affismart.mall.modules.product.dto.request.UpsertProductRequest;
import com.affismart.mall.modules.product.dto.response.ProductResponse;
import com.affismart.mall.modules.product.entity.Category;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.product.repository.CategoryRepository;
import com.affismart.mall.modules.product.repository.ProductRepository;
import java.math.BigDecimal;
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
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private ProductService productService;

	@Captor
	private ArgumentCaptor<Product> productCaptor;

	// =========================================================
	// createProduct()
	// =========================================================

	@Test
	@DisplayName("createProduct: Happy Path - product is created with normalized fields and generated slug")
	void createProduct_ValidRequest_CorrectlyNormalizesAndPersists() {
		// Given
		Category category = createMockCategory(1L, "Electronics");
		UpsertProductRequest request = new UpsertProductRequest(
				1L,
				" Tai nghe Bluetooth ",
				" sk-001 ",
				null,          // slug is null → should be generated from name
				" Mo ta ",
				new BigDecimal("199000"),
				5,
				" https://example.com/image.png "
		);
		Product savedProduct = buildSavedProduct(11L, category, "Tai nghe Bluetooth",
				"SK-001", "tai-nghe-bluetooth", new BigDecimal("199000"), 5, true);

		given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
		given(productRepository.existsBySkuIgnoreCase("SK-001")).willReturn(false);
		given(productRepository.existsBySlugIgnoreCase("tai-nghe-bluetooth")).willReturn(false);
		given(productRepository.save(any(Product.class))).willReturn(savedProduct);

		// When
		ProductResponse result = productService.createProduct(request);

		// Then - verify SKU is uppercased, slug is generated, name is trimmed
		verify(productRepository).save(productCaptor.capture());
		Product captured = productCaptor.getValue();
		assertThat(captured.getSku()).isEqualTo("SK-001");
		assertThat(captured.getSlug()).isEqualTo("tai-nghe-bluetooth");
		assertThat(captured.getName()).isEqualTo("Tai nghe Bluetooth");
		assertThat(captured.getImageUrl()).isEqualTo("https://example.com/image.png");
		assertThat(captured.isActive()).isTrue();
		assertThat(result.slug()).isEqualTo("tai-nghe-bluetooth");
	}

	@Test
	@DisplayName("createProduct: Exception - category not found throws CATEGORY_NOT_FOUND")
	void createProduct_CategoryNotFound_ThrowsCategoryNotFound() {
		// Given
		UpsertProductRequest request = new UpsertProductRequest(
				99L, "Product A", "SKU-A", "product-a",
				null, new BigDecimal("1000"), 1, null
		);
		given(categoryRepository.findById(99L)).willReturn(Optional.empty());

		// When + Then
		assertThatThrownBy(() -> productService.createProduct(request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);

		// Verify product was never saved when category is invalid
		verify(productRepository, never()).save(any());
	}

	@Test
	@DisplayName("createProduct: Exception - duplicate SKU throws PRODUCT_SKU_ALREADY_EXISTS")
	void createProduct_DuplicateSku_ThrowsSkuConflict() {
		// Given
		Category category = createMockCategory(1L, "Electronics");
		UpsertProductRequest request = new UpsertProductRequest(
				1L, "Product A", "DUP-SKU", "product-a",
				null, new BigDecimal("1000"), 1, null
		);

		given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
		given(productRepository.existsBySkuIgnoreCase("DUP-SKU")).willReturn(true);

		// When + Then
		assertThatThrownBy(() -> productService.createProduct(request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);

		verify(productRepository, never()).save(any());
	}

	@Test
	@DisplayName("createProduct: Exception - duplicate slug throws PRODUCT_SLUG_ALREADY_EXISTS")
	void createProduct_DuplicateSlug_ThrowsSlugConflict() {
		// Given
		Category category = createMockCategory(1L, "Electronics");
		UpsertProductRequest request = new UpsertProductRequest(
				1L, "Product B", "SKU-B", "shared-slug",
				null, new BigDecimal("2000"), 2, null
		);

		given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
		given(productRepository.existsBySkuIgnoreCase("SKU-B")).willReturn(false);
		given(productRepository.existsBySlugIgnoreCase("shared-slug")).willReturn(true);

		// When + Then
		assertThatThrownBy(() -> productService.createProduct(request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);

		verify(productRepository, never()).save(any());
	}

	// =========================================================
	// updateProduct()
	// =========================================================

	@Test
	@DisplayName("updateProduct: Happy Path - all fields are updated successfully")
	void updateProduct_ValidRequest_UpdatesAllFields() {
		// Given
		Long productId = 7L;
		Category category = createMockCategory(1L, "Electronics");
		Product existingProduct = buildSavedProduct(productId, category,
				"Old Product", "OLD-SKU", "old-product", new BigDecimal("1000"), 10, true);

		UpsertProductRequest request = new UpsertProductRequest(
				1L, "New Product", "NEW-SKU", "new-product",
				"New description", new BigDecimal("2500"), 20, null
		);
		Product savedProduct = buildSavedProduct(productId, category,
				"New Product", "NEW-SKU", "new-product", new BigDecimal("2500"), 20, true);

		given(productRepository.findById(productId)).willReturn(Optional.of(existingProduct));
		given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
		given(productRepository.existsBySkuIgnoreCaseAndIdNot("NEW-SKU", productId)).willReturn(false);
		given(productRepository.existsBySlugIgnoreCaseAndIdNot("new-product", productId)).willReturn(false);
		given(productRepository.save(any(Product.class))).willReturn(savedProduct);

		// When
		ProductResponse result = productService.updateProduct(productId, request);

		// Then - verify all updated fields were persisted
		verify(productRepository).save(productCaptor.capture());
		assertThat(productCaptor.getValue().getName()).isEqualTo("New Product");
		assertThat(productCaptor.getValue().getSku()).isEqualTo("NEW-SKU");
		assertThat(result.name()).isEqualTo("New Product");
	}

	@Test
	@DisplayName("updateProduct: Exception - duplicate SKU throws PRODUCT_SKU_ALREADY_EXISTS")
	void updateProduct_DuplicateSku_ThrowsConflict() {
		// Given
		Long productId = 7L;
		Category category = createMockCategory(1L, "Electronics");
		Product existingProduct = buildSavedProduct(productId, category,
				"Old Product", "OLD-SKU", "old-product", BigDecimal.TEN, 5, true);

		UpsertProductRequest request = new UpsertProductRequest(
				1L, "New Product", "dup-sku", "new-product",
				null, new BigDecimal("1500"), 3, null
		);

		given(productRepository.findById(productId)).willReturn(Optional.of(existingProduct));
		given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
		given(productRepository.existsBySkuIgnoreCaseAndIdNot("DUP-SKU", productId)).willReturn(true);

		// When + Then
		assertThatThrownBy(() -> productService.updateProduct(productId, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);

		verify(productRepository, never()).save(any());
	}

	@Test
	@DisplayName("updateProduct: Exception - duplicate slug throws PRODUCT_SLUG_ALREADY_EXISTS")
	void updateProduct_DuplicateSlug_ThrowsConflict() {
		// Given
		Long productId = 7L;
		Category category = createMockCategory(1L, "Electronics");
		Product existingProduct = buildSavedProduct(productId, category,
				"Old Product", "OLD-SKU", "old-product", BigDecimal.TEN, 5, true);

		UpsertProductRequest request = new UpsertProductRequest(
				1L, "New Product", "UNIQUE-SKU", "shared-slug",
				null, new BigDecimal("1500"), 3, null
		);

		given(productRepository.findById(productId)).willReturn(Optional.of(existingProduct));
		given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
		given(productRepository.existsBySkuIgnoreCaseAndIdNot("UNIQUE-SKU", productId)).willReturn(false);
		given(productRepository.existsBySlugIgnoreCaseAndIdNot("shared-slug", productId)).willReturn(true);

		// When + Then
		assertThatThrownBy(() -> productService.updateProduct(productId, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);

		verify(productRepository, never()).save(any());
	}

	@Test
	@DisplayName("updateProduct: Exception - product not found throws PRODUCT_NOT_FOUND")
	void updateProduct_NotFound_ThrowsProductNotFound() {
		// Given
		UpsertProductRequest request = new UpsertProductRequest(
				1L, "Any", "SKU", "any",
				null, BigDecimal.ONE, 1, null
		);
		given(productRepository.findById(99L)).willReturn(Optional.empty());

		// When + Then
		assertThatThrownBy(() -> productService.updateProduct(99L, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
	}

	// =========================================================
	// updateProductStatus()
	// =========================================================

	@Test
	@DisplayName("updateProductStatus: Happy Path - product is deactivated (soft delete)")
	void updateProductStatus_SetInactive_PersistsChange() {
		// Given
		Long productId = 5L;
		Category category = createMockCategory(1L, "Electronics");
		Product activeProduct = buildSavedProduct(productId, category,
				"Product A", "SKU-A", "product-a", BigDecimal.TEN, 10, true);
		UpdateProductStatusRequest request = new UpdateProductStatusRequest(false);

		Product savedProduct = buildSavedProduct(productId, category,
				"Product A", "SKU-A", "product-a", BigDecimal.TEN, 10, false);

		given(productRepository.findById(productId)).willReturn(Optional.of(activeProduct));
		given(productRepository.save(any(Product.class))).willReturn(savedProduct);

		// When
		ProductResponse result = productService.updateProductStatus(productId, request);

		// Then - verify is_active was set to false (soft delete, not hard delete)
		verify(productRepository).save(productCaptor.capture());
		assertThat(productCaptor.getValue().isActive()).isFalse();
		assertThat(result.active()).isFalse();
	}

	@Test
	@DisplayName("updateProductStatus: Exception - product not found throws PRODUCT_NOT_FOUND")
	void updateProductStatus_NotFound_ThrowsProductNotFound() {
		// Given
		UpdateProductStatusRequest request = new UpdateProductStatusRequest(false);
		given(productRepository.findById(99L)).willReturn(Optional.empty());

		// When + Then
		assertThatThrownBy(() -> productService.updateProductStatus(99L, request))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
	}

	// =========================================================
	// Private Helper Methods
	// =========================================================

	private Category createMockCategory(Long id, String name) {
		Category category = new Category();
		category.setId(id);
		category.setName(name);
		category.setSlug(name.toLowerCase().replace(" ", "-"));
		category.setActive(true);
		return category;
	}

	private Product buildSavedProduct(
			Long id, Category category, String name,
			String sku, String slug, BigDecimal price,
			int stock, boolean active
	) {
		Product product = new Product();
		product.setId(id);
		product.setCategory(category);
		product.setName(name);
		product.setSku(sku);
		product.setSlug(slug);
		product.setPrice(price);
		product.setStockQuantity(stock);
		product.setActive(active);
		return product;
	}
}
