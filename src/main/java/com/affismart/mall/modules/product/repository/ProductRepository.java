package com.affismart.mall.modules.product.repository;

import com.affismart.mall.modules.product.entity.Product;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	Optional<Product> findBySlug(String slug);

	Optional<Product> findByIdAndActiveTrue(Long id);

	@EntityGraph(attributePaths = {"category"})
	Optional<Product> findBySlugAndActiveTrue(String slug);

	Page<Product> findAllByActiveTrue(Pageable pageable);

	Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"category"})
	Page<Product> findAll(Specification<Product> specification, Pageable pageable);

	@EntityGraph(attributePaths = {"category"})
	@Query("SELECT p FROM Product p WHERE p.id = :id")
	Optional<Product> findWithCategoryById(@Param("id") Long id);

	boolean existsBySkuIgnoreCase(String sku);

	boolean existsBySlugIgnoreCase(String slug);

	boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

	boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Product p WHERE p.id IN :ids")
	List<Product> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

	@Query("""
			SELECT p
			FROM Product p
			JOIN FETCH p.category
			WHERE p.active = true
			  AND p.stockQuantity < :threshold
			ORDER BY p.stockQuantity ASC, p.updatedAt DESC
			""")
	List<Product> findLowStockProducts(@Param("threshold") int threshold);
}
