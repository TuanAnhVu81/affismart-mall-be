package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.ReferralLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralLinkRepository extends JpaRepository<ReferralLink, Long> {

	boolean existsByRefCode(String refCode);

	List<ReferralLink> findByAffiliateAccount_IdOrderByCreatedAtDesc(Long affiliateAccountId);
}
