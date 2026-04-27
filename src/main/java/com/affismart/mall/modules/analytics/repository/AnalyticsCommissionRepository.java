package com.affismart.mall.modules.analytics.repository;

import com.affismart.mall.common.enums.CommissionStatus;
import com.affismart.mall.modules.affiliate.entity.Commission;
import com.affismart.mall.modules.analytics.projection.TopAffiliateProjection;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsCommissionRepository extends JpaRepository<Commission, Long> {

	@Query("""
			SELECT aa.id AS affiliateAccountId,
			       aa.user.id AS userId,
			       aa.user.fullName AS fullName,
			       aa.refCode AS refCode,
			       COUNT(c.id) AS conversionCount,
			       COALESCE(SUM(c.amount), 0) AS totalCommission,
			       COALESCE(SUM(c.order.totalAmount), 0) AS attributedRevenue
			FROM Commission c
			JOIN c.affiliateAccount aa
			WHERE c.status IN :statuses
			GROUP BY aa.id, aa.user.id, aa.user.fullName, aa.refCode
			ORDER BY SUM(c.order.totalAmount) DESC, SUM(c.amount) DESC
			""")
	List<TopAffiliateProjection> findTopAffiliates(
			@Param("statuses") Collection<CommissionStatus> statuses,
			Pageable pageable
	);
}
