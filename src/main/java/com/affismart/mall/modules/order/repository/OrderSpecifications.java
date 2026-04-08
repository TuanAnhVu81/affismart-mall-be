package com.affismart.mall.modules.order.repository;

import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.modules.order.entity.Order;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ObjectUtils;

public final class OrderSpecifications {

	private OrderSpecifications() {
	}

	public static Specification<Order> forAdminManagement(
			OrderStatus status,
			LocalDateTime createdFrom,
			LocalDateTime createdTo
	) {
		return hasStatus(status)
				.and(createdAfterOrAt(createdFrom))
				.and(createdBeforeOrAt(createdTo));
	}

	private static Specification<Order> hasStatus(OrderStatus status) {
		return (root, query, criteriaBuilder) -> {
			if (status == null) {
				return criteriaBuilder.conjunction();
			}
			return criteriaBuilder.equal(root.get("status"), status);
		};
	}

	private static Specification<Order> createdAfterOrAt(LocalDateTime createdFrom) {
		return (root, query, criteriaBuilder) -> {
			if (ObjectUtils.isEmpty(createdFrom)) {
				return criteriaBuilder.conjunction();
			}
			return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
		};
	}

	private static Specification<Order> createdBeforeOrAt(LocalDateTime createdTo) {
		return (root, query, criteriaBuilder) -> {
			if (ObjectUtils.isEmpty(createdTo)) {
				return criteriaBuilder.conjunction();
			}
			return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
		};
	}
}
