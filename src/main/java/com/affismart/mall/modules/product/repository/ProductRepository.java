package com.affismart.mall.modules.product.repository;

import com.affismart.mall.modules.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	Optional<Product> findBySlug(String slug);

	Optional<Product> findByIdAndActiveTrue(Long id);

	Optional<Product> findBySlugAndActiveTrue(String slug);

	Page<Product> findAllByActiveTrue(Pageable pageable);

	Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

	boolean existsBySkuIgnoreCase(String sku);

	boolean existsBySlugIgnoreCase(String slug);

	boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

	boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

	@Query(
			value = """
					SELECT p.*
					FROM products p
					WHERE p.is_active = true
					  AND p.stock_quantity < :threshold
					ORDER BY p.stock_quantity ASC, p.updated_at DESC
					""",
			nativeQuery = true
	)
	List<Product> findLowStockProducts(@Param("threshold") int threshold);
}
