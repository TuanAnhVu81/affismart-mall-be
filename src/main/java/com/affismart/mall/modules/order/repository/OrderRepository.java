package com.affismart.mall.modules.order.repository;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.modules.order.entity.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findByUserId(Long userId, Pageable pageable);

	Optional<Order> findByIdAndUserId(Long id, Long userId);

	Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
