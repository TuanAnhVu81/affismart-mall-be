package com.affismart.mall.modules.order.repository;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.modules.order.entity.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

	Page<Order> findByUser_Id(Long userId, Pageable pageable);

	Optional<Order> findByIdAndUser_Id(Long id, Long userId);

	Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
