package com.affismart.mall.modules.product.repository;

import com.affismart.mall.modules.product.entity.Product;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

	private ProductSpecifications() {
	}

	public static Specification<Product> forPublicCatalog(
			String keyword,
			Long categoryId,
			BigDecimal minPrice,
			BigDecimal maxPrice
	) {
		Specification<Product> specification = activeOnly();
		specification = specification.and(keywordContains(keyword));
		specification = specification.and(hasCategoryId(categoryId));
		specification = specification.and(priceFrom(minPrice));
		specification = specification.and(priceTo(maxPrice));
		return specification;
	}

	private static Specification<Product> activeOnly() {
		return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("active"));
	}

	private static Specification<Product> keywordContains(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return null;
		}

		String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
		return (root, query, criteriaBuilder) -> criteriaBuilder.or(
				criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
				criteriaBuilder.like(
						criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")),
						pattern
				)
		);
	}

	private static Specification<Product> hasCategoryId(Long categoryId) {
		if (categoryId == null) {
			return null;
		}
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category").get("id"), categoryId);
	}

	private static Specification<Product> priceFrom(BigDecimal minPrice) {
		if (minPrice == null) {
			return null;
		}
		return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
	}

	private static Specification<Product> priceTo(BigDecimal maxPrice) {
		if (maxPrice == null) {
			return null;
		}
		return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
	}
}
