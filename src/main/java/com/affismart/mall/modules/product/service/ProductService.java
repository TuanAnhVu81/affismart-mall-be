package com.affismart.mall.modules.product.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.common.response.PageResponse;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.product.dto.request.UpdateProductStatusRequest;
import com.affismart.mall.modules.product.dto.request.UpsertProductRequest;
import com.affismart.mall.modules.product.dto.response.ProductResponse;
import com.affismart.mall.modules.product.entity.Category;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.product.repository.CategoryRepository;
import com.affismart.mall.modules.product.repository.ProductRepository;
import com.affismart.mall.modules.product.repository.ProductSpecifications;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductService {

	private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;

	public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
	}

	@Transactional
	public ProductResponse createProduct(UpsertProductRequest request) {
		Category category = getRequiredCategory(request.categoryId());
		String normalizedName = normalizeName(request.name());
		String normalizedSku = normalizeSku(request.sku());
		String resolvedSlug = resolveSlug(request.slug(), normalizedName);

		ensureUniqueForCreate(normalizedSku, resolvedSlug);

		Product product = new Product();
		applyProductFields(product, request, category, normalizedName, normalizedSku, resolvedSlug);
		product.setActive(true);

		return toResponse(productRepository.save(product));
	}

	@Transactional
	public ProductResponse updateProduct(Long productId, UpsertProductRequest request) {
		Product product = getRequiredProduct(productId);
		Category category = getRequiredCategory(request.categoryId());
		String normalizedName = normalizeName(request.name());
		String normalizedSku = normalizeSku(request.sku());
		String resolvedSlug = resolveSlug(request.slug(), normalizedName);

		ensureUniqueForUpdate(normalizedSku, resolvedSlug, productId);

		applyProductFields(product, request, category, normalizedName, normalizedSku, resolvedSlug);
		return toResponse(productRepository.save(product));
	}

	@Transactional
	public ProductResponse updateProductStatus(Long productId, UpdateProductStatusRequest request) {
		Product product = getRequiredProduct(productId);
		product.setActive(request.active());
		return toResponse(productRepository.save(product));
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> getPublicProducts(
			int page,
			int size,
			String sortBy,
			String keyword,
			Long categoryId,
			BigDecimal minPrice,
			BigDecimal maxPrice
	) {
		validatePriceRange(minPrice, maxPrice);

		Pageable pageable = PageRequest.of(
				Math.max(page, 0),
				normalizePageSize(size),
				resolvePublicSort(sortBy)
		);

		Page<ProductResponse> result = productRepository.findAll(
						ProductSpecifications.forPublicCatalog(keyword, categoryId, minPrice, maxPrice),
						pageable
				)
				.map(this::toResponse);
		return PageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public ProductResponse getActiveProductById(Long productId) {
		Product product = productRepository.findByIdAndActiveTrue(productId)
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
		return toResponse(product);
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> getLowStockProducts() {
		return getLowStockProducts(DEFAULT_LOW_STOCK_THRESHOLD);
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> getLowStockProducts(int threshold) {
		int normalizedThreshold = Math.max(threshold, 1);
		return productRepository.findLowStockProducts(normalizedThreshold)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	private void applyProductFields(
			Product product,
			UpsertProductRequest request,
			Category category,
			String normalizedName,
			String normalizedSku,
			String resolvedSlug
	) {
		product.setCategory(category);
		product.setName(normalizedName);
		product.setSku(normalizedSku);
		product.setSlug(resolvedSlug);
		product.setDescription(normalizeOptionalText(request.description()));
		product.setPrice(request.price());
		product.setStockQuantity(request.stockQuantity());
		product.setImageUrl(normalizeOptionalText(request.imageUrl()));
	}

	private Product getRequiredProduct(Long productId) {
		return productRepository.findById(productId)
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
	}

	private Category getRequiredCategory(Long categoryId) {
		return categoryRepository.findById(categoryId)
				.orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
	}

	private String normalizeName(String name) {
		return name == null ? null : name.trim();
	}

	private String normalizeSku(String sku) {
		return sku == null ? null : sku.trim().toUpperCase(Locale.ROOT);
	}

	private String resolveSlug(String slugInput, String fallbackName) {
		String rawSlug = StringUtils.hasText(slugInput) ? slugInput.trim() : fallbackName;
		String resolvedSlug = SlugUtils.toSlug(rawSlug);
		if (!StringUtils.hasText(resolvedSlug)) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Product slug is invalid");
		}
		return resolvedSlug;
	}

	private String normalizeOptionalText(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private Sort resolvePublicSort(String sortBy) {
		if (!StringUtils.hasText(sortBy)) {
			return Sort.by(Sort.Direction.DESC, "createdAt");
		}

		return switch (sortBy.trim().toLowerCase(Locale.ROOT)) {
			case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
			case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
			case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
			default -> Sort.by(Sort.Direction.DESC, "createdAt");
		};
	}

	private int normalizePageSize(int size) {
		if (size <= 0) {
			return 10;
		}
		return Math.min(size, 100);
	}

	private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
		if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
			throw new AppException(ErrorCode.INVALID_INPUT, "minPrice must be less than or equal to maxPrice");
		}
	}

	private void ensureUniqueForCreate(String sku, String slug) {
		if (productRepository.existsBySkuIgnoreCase(sku)) {
			throw new AppException(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
		}
		if (productRepository.existsBySlugIgnoreCase(slug)) {
			throw new AppException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
		}
	}

	private void ensureUniqueForUpdate(String sku, String slug, Long productId) {
		if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, productId)) {
			throw new AppException(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
		}
		if (productRepository.existsBySlugIgnoreCaseAndIdNot(slug, productId)) {
			throw new AppException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
		}
	}

	private ProductResponse toResponse(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getCategory().getId(),
				product.getCategory().getName(),
				product.getName(),
				product.getSku(),
				product.getSlug(),
				product.getDescription(),
				product.getPrice(),
				product.getStockQuantity(),
				product.getImageUrl(),
				product.isActive(),
				product.getCreatedAt(),
				product.getUpdatedAt()
		);
	}
}
