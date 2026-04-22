package com.affismart.mall.modules.product.repository;

import com.affismart.mall.modules.product.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findBySlug(String slug);

	Optional<Category> findByIdAndActiveTrue(Long id);

	Optional<Category> findBySlugAndActiveTrue(String slug);

	List<Category> findAllByActiveTrueOrderByNameAsc();

	List<Category> findAllByActiveOrderByNameAsc(boolean active);

	List<Category> findAllByOrderByNameAsc();

	boolean existsBySlugIgnoreCase(String slug);

	boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);
}
