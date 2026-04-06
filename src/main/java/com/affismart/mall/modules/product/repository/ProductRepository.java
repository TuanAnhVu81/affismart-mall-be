package com.affismart.mall.modules.product.repository;

import com.affismart.mall.modules.product.entity.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
