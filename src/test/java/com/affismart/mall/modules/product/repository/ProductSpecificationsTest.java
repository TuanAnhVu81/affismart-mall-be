package com.affismart.mall.modules.product.repository;

import com.affismart.mall.modules.product.entity.Category;
import com.affismart.mall.modules.product.entity.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProductSpecifications Unit Tests")
class ProductSpecificationsTest {

	@Test
	@DisplayName("forPublicCatalog: keyword searches product identity fields and skips description")
	@SuppressWarnings({"unchecked", "rawtypes"})
	void forPublicCatalog_WithKeyword_DoesNotSearchDescription() {
		Root<Product> root = mock(Root.class);
		CriteriaQuery<?> query = mock(CriteriaQuery.class);
		CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

		Path<String> activePath = mock(Path.class);
		Path<String> namePath = mock(Path.class);
		Path<String> skuPath = mock(Path.class);
		Path<String> slugPath = mock(Path.class);
		Path<Category> categoryPath = mock(Path.class);
		Path<String> categoryNamePath = mock(Path.class);
		Expression<String> lowerName = mock(Expression.class);
		Expression<String> lowerSku = mock(Expression.class);
		Expression<String> lowerSlug = mock(Expression.class);
		Expression<String> lowerCategoryName = mock(Expression.class);
		Predicate activePredicate = mock(Predicate.class);
		Predicate namePredicate = mock(Predicate.class);
		Predicate skuPredicate = mock(Predicate.class);
		Predicate slugPredicate = mock(Predicate.class);
		Predicate categoryPredicate = mock(Predicate.class);
		Predicate keywordPredicate = mock(Predicate.class);

		when(root.get("active")).thenReturn((Path) activePath);
		when(root.get("name")).thenReturn((Path) namePath);
		when(root.get("sku")).thenReturn((Path) skuPath);
		when(root.get("slug")).thenReturn((Path) slugPath);
		when(root.get("category")).thenReturn((Path) categoryPath);
		when(categoryPath.get("name")).thenReturn((Path) categoryNamePath);
		when(criteriaBuilder.isTrue((Expression<Boolean>) (Expression) activePath)).thenReturn(activePredicate);
		when(criteriaBuilder.lower(namePath)).thenReturn(lowerName);
		when(criteriaBuilder.lower(skuPath)).thenReturn(lowerSku);
		when(criteriaBuilder.lower(slugPath)).thenReturn(lowerSlug);
		when(criteriaBuilder.lower(categoryNamePath)).thenReturn(lowerCategoryName);
		when(criteriaBuilder.like(lowerName, "%ab%")).thenReturn(namePredicate);
		when(criteriaBuilder.like(lowerSku, "%ab%")).thenReturn(skuPredicate);
		when(criteriaBuilder.like(lowerSlug, "%ab%")).thenReturn(slugPredicate);
		when(criteriaBuilder.like(lowerCategoryName, "%ab%")).thenReturn(categoryPredicate);
		when(criteriaBuilder.or(namePredicate, skuPredicate, slugPredicate, categoryPredicate)).thenReturn(keywordPredicate);
		when(criteriaBuilder.and(activePredicate, keywordPredicate)).thenReturn(keywordPredicate);

		Specification<Product> specification = ProductSpecifications.forPublicCatalog(" ab ", null, null, null);

		Predicate result = specification.toPredicate(root, query, criteriaBuilder);

		assertThat(result).isSameAs(keywordPredicate);
		verify(root).get("name");
		verify(root).get("sku");
		verify(root).get("slug");
		verify(root).get("category");
		verify(root, never()).get("description");
	}

	@Test
	@DisplayName("forPublicCatalog: blank keyword keeps only non-keyword filters")
	@SuppressWarnings({"unchecked", "rawtypes"})
	void forPublicCatalog_BlankKeyword_DoesNotBuildKeywordPredicate() {
		Root<Product> root = mock(Root.class);
		CriteriaQuery<?> query = mock(CriteriaQuery.class);
		CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
		Path<String> activePath = mock(Path.class);
		Predicate activePredicate = mock(Predicate.class);

		when(root.get("active")).thenReturn((Path) activePath);
		when(criteriaBuilder.isTrue((Expression<Boolean>) (Expression) activePath)).thenReturn(activePredicate);

		Specification<Product> specification = ProductSpecifications.forPublicCatalog("   ", null, null, null);

		Predicate result = specification.toPredicate(root, query, criteriaBuilder);

		assertThat(result).isSameAs(activePredicate);
		verify(root, never()).get("description");
		verify(criteriaBuilder, never()).like(any(), eq("%%"));
	}
}
