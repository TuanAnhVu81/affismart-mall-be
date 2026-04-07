package com.affismart.mall.modules.order.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AffiliateAccountLookupRepository {

	private static final String LOOKUP_APPROVED_AFFILIATE_SQL = """
			SELECT id
			FROM affiliate_accounts
			WHERE ref_code = ?
			  AND status = 'APPROVED'
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
						refCode
				)
				.stream()
				.findFirst();
	}
}
