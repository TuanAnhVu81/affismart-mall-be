package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.ReferralLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReferralLinkRepository extends JpaRepository<ReferralLink, Long> {

	boolean existsByRefCode(String refCode);

	List<ReferralLink> findByAffiliateAccount_IdOrderByCreatedAtDesc(Long affiliateAccountId);

	@Modifying
	@Query(
			value = """
					UPDATE referral_links
					SET total_clicks = total_clicks + 1,
					    updated_at = CURRENT_TIMESTAMP
					WHERE ref_code = :refCode
					  AND is_active = true
					""",
			nativeQuery = true
	)
	int incrementTotalClicksByRefCode(@Param("refCode") String refCode);
}
