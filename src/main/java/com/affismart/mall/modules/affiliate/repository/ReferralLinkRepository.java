package com.affismart.mall.modules.affiliate.repository;

import com.affismart.mall.modules.affiliate.entity.ReferralLink;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReferralLinkRepository extends JpaRepository<ReferralLink, Long> {

	boolean existsByRefCode(String refCode);

	@EntityGraph(attributePaths = {"product"})
	@Query("""
			SELECT rl
			FROM ReferralLink rl
			WHERE rl.affiliateAccount.id = :affiliateAccountId
			  AND (:active IS NULL OR rl.active = :active)
			""")
	Page<ReferralLink> findByAffiliateAccountId(
			@Param("affiliateAccountId") Long affiliateAccountId,
			@Param("active") Boolean active,
			Pageable pageable
	);

	@EntityGraph(attributePaths = {"product"})
	Optional<ReferralLink> findByIdAndAffiliateAccount_Id(Long id, Long affiliateAccountId);

	@Query("""
			SELECT COALESCE(SUM(rl.totalClicks), 0)
			FROM ReferralLink rl
			WHERE rl.affiliateAccount.id = :affiliateAccountId
			""")
	Long sumTotalClicksByAffiliateAccountId(@Param("affiliateAccountId") Long affiliateAccountId);

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

	@Modifying
	@Query(
			value = """
					UPDATE referral_links
					SET total_conversions = total_conversions + 1,
					    updated_at = CURRENT_TIMESTAMP
					WHERE id = :referralLinkId
					""",
			nativeQuery = true
	)
	int incrementTotalConversionsById(@Param("referralLinkId") Long referralLinkId);
}
