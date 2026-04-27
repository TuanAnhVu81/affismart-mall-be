package com.affismart.mall.modules.analytics.repository;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.modules.affiliate.entity.AffiliateAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsAffiliateRepository extends JpaRepository<AffiliateAccount, Long> {

	long countByStatus(AffiliateAccountStatus status);
}
