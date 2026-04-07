package com.affismart.mall.modules.order.repository;

import com.affismart.mall.modules.order.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	List<OrderItem> findByOrder_Id(Long orderId);

	@Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id = :orderId")
	List<OrderItem> findByOrder_IdWithProduct(@Param("orderId") Long orderId);
}
