package com.affismart.mall.modules.analytics.repository;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.modules.analytics.projection.DashboardOrderMetricsProjection;
import com.affismart.mall.modules.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsOrderRepository extends JpaRepository<Order, Long> {

	@Query("""
			SELECT COALESCE(SUM(o.totalAmount), 0) AS grossMerchandiseValue,
			       COUNT(o) AS completedOrders
			FROM Order o
			WHERE o.status = :status
			""")
	DashboardOrderMetricsProjection getOrderMetricsByStatus(@Param("status") OrderStatus status);
}
