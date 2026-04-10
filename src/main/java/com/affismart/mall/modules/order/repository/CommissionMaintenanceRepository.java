package com.affismart.mall.modules.order.repository;

import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CommissionMaintenanceRepository {

	private static final String REJECT_PENDING_BY_ORDER_SQL = """
			UPDATE commissions
			SET status = 'REJECTED',
			    updated_at = CURRENT_TIMESTAMP
			WHERE order_id = ?
			  AND status = 'PENDING'
			""";

	private static final String INSERT_PENDING_COMMISSION_SQL = """
			INSERT INTO commissions (
			    affiliate_account_id,
			    order_id,
			    amount,
			    rate_snapshot,
			    status,
			    created_at,
			    updated_at
			)
			SELECT aa.id,
			       ?,
			       ROUND((? * aa.commission_rate) / 100.0, 2),
			       aa.commission_rate,
			       'PENDING',
			       CURRENT_TIMESTAMP,
			       CURRENT_TIMESTAMP
			FROM affiliate_accounts aa
			WHERE aa.id = ?
			  AND aa.status = 'APPROVED'
			ON CONFLICT (order_id) DO NOTHING
			""";

	private final JdbcTemplate jdbcTemplate;

	public CommissionMaintenanceRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int rejectPendingCommissionByOrderId(Long orderId) {
		return jdbcTemplate.update(REJECT_PENDING_BY_ORDER_SQL, orderId);
	}

	public int insertPendingCommission(Long affiliateAccountId, Long orderId, BigDecimal orderTotalAmount) {
		return jdbcTemplate.update(
				INSERT_PENDING_COMMISSION_SQL,
				orderId,
				orderTotalAmount,
				affiliateAccountId
		);
	}
}
