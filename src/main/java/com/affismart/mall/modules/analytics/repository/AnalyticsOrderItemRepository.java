package com.affismart.mall.modules.analytics.repository;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.modules.analytics.projection.TopProductProjection;
import com.affismart.mall.modules.order.entity.OrderItem;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsOrderItemRepository extends JpaRepository<OrderItem, Long> {

	@Query("""
			SELECT oi.product.id AS productId,
			       oi.product.name AS productName,
			       oi.product.sku AS sku,
			       oi.product.imageUrl AS imageUrl,
			       COALESCE(SUM(oi.quantity), 0) AS quantitySold,
			       COALESCE(SUM(oi.priceAtTime * oi.quantity), 0) AS revenue
			FROM OrderItem oi
			WHERE oi.order.status = :status
			GROUP BY oi.product.id, oi.product.name, oi.product.sku, oi.product.imageUrl
			ORDER BY SUM(oi.quantity) DESC, SUM(oi.priceAtTime * oi.quantity) DESC
			""")
	List<TopProductProjection> findTopProducts(
			@Param("status") OrderStatus status,
			Pageable pageable
	);
}
