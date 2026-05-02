package com.affismart.mall.modules.product.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.common.response.PageResponse;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.product.dto.request.UpdateProductStatusRequest;
import com.affismart.mall.modules.product.dto.request.UpsertProductRequest;
import com.affismart.mall.modules.product.dto.response.ProductResponse;
import com.affismart.mall.modules.product.entity.Category;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.product.mapper.ProductMapper;
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
	private final ProductMapper productMapper;

	public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, ProductMapper productMapper) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.productMapper = productMapper;
	}

	@Transactional
	public ProductResponse createProduct(UpsertProductRequest request) {
		Category category = getRequiredCategory(request.categoryId());
		UpsertProductRequest normalizedRequest = normalizeAndResolveRequest(request);

		ensureUniqueForCreate(normalizedRequest.sku(), normalizedRequest.slug());

		Product product = productMapper.toProductEntity(normalizedRequest);
		product.setCategory(category);
		product.setActive(true);

		return productMapper.toProductResponse(productRepository.save(product));
	}

	@Transactional
	public ProductResponse updateProduct(Long productId, UpsertProductRequest request) {
		Product product = getRequiredProduct(productId);
		Category category = getRequiredCategory(request.categoryId());
		UpsertProductRequest normalizedRequest = normalizeAndResolveRequest(request);

		ensureUniqueForUpdate(normalizedRequest.sku(), normalizedRequest.slug(), productId);

		productMapper.updateProductFromRequest(normalizedRequest, product);
		product.setCategory(category);
		return productMapper.toProductResponse(productRepository.save(product));
	}

	@Transactional
	public ProductResponse updateProductStatus(Long productId, UpdateProductStatusRequest request) {
		Product product = getRequiredProduct(productId);
		product.setActive(request.active());
		return productMapper.toProductResponse(productRepository.save(product));
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> getPublicProducts(
			int page,
			int size,
			String sortBy,
			Long categoryId,
			BigDecimal minPrice,
			BigDecimal maxPrice
	) {
		return getPublicProducts(page, size, sortBy, null, categoryId, minPrice, maxPrice);
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> searchPublicProducts(
			int page,
			int size,
			String sortBy,
			String keyword,
			Long categoryId,
			BigDecimal minPrice,
			BigDecimal maxPrice
	) {
		return getPublicProducts(page, size, sortBy, keyword, categoryId, minPrice, maxPrice);
	}

	private PageResponse<ProductResponse> getPublicProducts(
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
				.map(productMapper::toProductResponse);
		return PageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> getAdminProducts(
			int page,
			int size,
			String sortBy,
			String sortDir,
			String keyword,
			Long categoryId,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Boolean active
	) {
		validatePriceRange(minPrice, maxPrice);

		Pageable pageable = PageRequest.of(
				Math.max(page, 0),
				normalizePageSize(size),
				resolveAdminSort(sortBy, sortDir)
		);

		Page<ProductResponse> result = productRepository.findAll(
						ProductSpecifications.forAdminCatalog(keyword, categoryId, minPrice, maxPrice, active),
						pageable
				)
				.map(productMapper::toProductResponse);
		return PageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public ProductResponse getActiveProductBySlug(String slug) {
		Product product = productRepository.findBySlugAndActiveTrue(slug)
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
		return productMapper.toProductResponse(product);
	}

	@Transactional(readOnly = true)
	public ProductResponse getAdminProductById(Long productId) {
		Product product = productRepository.findWithCategoryById(productId)
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
		return productMapper.toProductResponse(product);
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
				.map(productMapper::toProductResponse)
				.toList();
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

	private UpsertProductRequest normalizeAndResolveRequest(UpsertProductRequest request) {
		String normalizedName = normalizeName(request.name());
		String normalizedSku = normalizeSku(request.sku());
		String resolvedSlug = resolveSlug(request.slug(), normalizedName);

		return new UpsertProductRequest(
				request.categoryId(),
				normalizedName,
				normalizedSku,
				resolvedSlug,
				normalizeOptionalText(request.description()),
				request.price(),
				request.stockQuantity(),
				normalizeOptionalText(request.imageUrl())
		);
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

	private Sort resolveAdminSort(String sortBy, String sortDir) {
		String normalizedSortBy = StringUtils.hasText(sortBy)
				? sortBy.trim().toLowerCase(Locale.ROOT)
				: "created_at";

		String property = switch (normalizedSortBy) {
			case "id" -> "id";
			case "name" -> "name";
			case "price" -> "price";
			case "stock_quantity" -> "stockQuantity";
			case "updated_at" -> "updatedAt";
			case "created_at", "newest" -> "createdAt";
			case "price_asc", "price_desc" -> "price";
			default -> "createdAt";
		};

		Sort.Direction direction = switch (normalizedSortBy) {
			case "price_asc" -> Sort.Direction.ASC;
			case "price_desc", "newest" -> Sort.Direction.DESC;
			default -> resolveSortDirection(sortDir);
		};

		return Sort.by(direction, property);
	}

	private Sort.Direction resolveSortDirection(String sortDir) {
		if (!StringUtils.hasText(sortDir)) {
			return Sort.Direction.DESC;
		}
		return "asc".equalsIgnoreCase(sortDir.trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;
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
}
