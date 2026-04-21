package com.affismart.mall.modules.order.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class AffiliateAccountLookupRepository {

	private static final String LOOKUP_ATTRIBUTION_SQL = """
			SELECT affiliate_account_id, referral_link_id
			FROM (
			    SELECT aa.id AS affiliate_account_id,
			           rl.id AS referral_link_id,
			           0 AS priority
			    FROM referral_links rl
			    JOIN affiliate_accounts aa
			      ON aa.id = rl.affiliate_account_id
			    WHERE rl.ref_code = ?
			      AND rl.is_active = true
			      AND aa.status = 'APPROVED'
			
			    UNION ALL
			
			    SELECT aa.id AS affiliate_account_id,
			           CAST(NULL AS BIGINT) AS referral_link_id,
			           1 AS priority
			    FROM affiliate_accounts aa
			    WHERE aa.ref_code = ?
			      AND aa.status = 'APPROVED'
			) matched_accounts
			ORDER BY priority
			LIMIT 1
			""";

	private final JdbcTemplate jdbcTemplate;

	public AffiliateAccountLookupRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<AffiliateAttribution> findAttributionByRefCode(String refCode) {
		return jdbcTemplate.query(
						LOOKUP_ATTRIBUTION_SQL,
				(resultSet, rowNum) -> mapAttribution(resultSet),
				refCode,
				refCode
		)
				.stream()
				.findFirst();
	}

	private AffiliateAttribution mapAttribution(ResultSet resultSet) throws SQLException {
		Long affiliateAccountId = resultSet.getLong("affiliate_account_id");
		Long referralLinkId = resultSet.getObject("referral_link_id", Long.class);
		return new AffiliateAttribution(affiliateAccountId, referralLinkId);
	}
}
