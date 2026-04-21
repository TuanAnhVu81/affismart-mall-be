package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.common.enums.CommissionStatus;
import com.affismart.mall.modules.affiliate.entity.Commission;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ObjectUtils;

public final class CommissionSpecifications {

	private CommissionSpecifications() {
	}

	public static Specification<Commission> forAffiliateManagement(
			Long affiliateAccountId,
			CommissionStatus status,
			LocalDateTime createdFrom,
			LocalDateTime createdTo
	) {
		return hasAffiliateAccountId(affiliateAccountId)
				.and(hasStatus(status))
				.and(createdAfterOrAt(createdFrom))
				.and(createdBeforeOrAt(createdTo));
	}

	private static Specification<Commission> hasAffiliateAccountId(Long affiliateAccountId) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
				root.get("affiliateAccount").get("id"),
				affiliateAccountId
		);
	}

	private static Specification<Commission> hasStatus(CommissionStatus status) {
		return (root, query, criteriaBuilder) -> {
			if (status == null) {
				return criteriaBuilder.conjunction();
			}
			return criteriaBuilder.equal(root.get("status"), status);
		};
	}

	private static Specification<Commission> createdAfterOrAt(LocalDateTime createdFrom) {
		return (root, query, criteriaBuilder) -> {
			if (ObjectUtils.isEmpty(createdFrom)) {
				return criteriaBuilder.conjunction();
			}
			return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
		};
	}

	private static Specification<Commission> createdBeforeOrAt(LocalDateTime createdTo) {
		return (root, query, criteriaBuilder) -> {
			if (ObjectUtils.isEmpty(createdTo)) {
				return criteriaBuilder.conjunction();
			}
			return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
		};
	}
}
