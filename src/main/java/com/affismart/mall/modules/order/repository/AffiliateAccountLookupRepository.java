package com.affismart.mall.modules.order.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AffiliateAccountLookupRepository {

	private static final String LOOKUP_APPROVED_AFFILIATE_SQL = """
			SELECT id
			FROM (
			    SELECT aa.id
			    FROM referral_links rl
			    JOIN affiliate_accounts aa
			      ON aa.id = rl.affiliate_account_id
			    WHERE rl.ref_code = ?
			      AND rl.is_active = true
			      AND aa.status = 'APPROVED'
			
			    UNION ALL
			
			    SELECT aa.id
			    FROM affiliate_accounts aa
			    WHERE aa.ref_code = ?
			      AND aa.status = 'APPROVED'
			) matched_accounts
			LIMIT 1
			""";

	private final JdbcTemplate jdbcTemplate;

	public AffiliateAccountLookupRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Long> findApprovedAccountIdByRefCode(String refCode) {
		return jdbcTemplate.query(
						LOOKUP_APPROVED_AFFILIATE_SQL,
				(resultSet, rowNum) -> resultSet.getLong("id"),
				refCode,
				refCode
		)
				.stream()
				.findFirst();
	}
}
