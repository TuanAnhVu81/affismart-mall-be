package com.affismart.mall.modules.order.repository;

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

	private static final String APPROVE_PENDING_AND_ADD_BALANCE_SQL = """
			WITH approved_commission AS (
			    UPDATE commissions
			    SET status = 'APPROVED',
			        updated_at = CURRENT_TIMESTAMP
			    WHERE order_id = ?
			      AND status = 'PENDING'
			    RETURNING affiliate_account_id, amount
			)
			UPDATE affiliate_accounts aa
			SET balance = aa.balance + ac.amount,
			    updated_at = CURRENT_TIMESTAMP
			FROM approved_commission ac
			WHERE aa.id = ac.affiliate_account_id
			""";

	private final JdbcTemplate jdbcTemplate;

	public CommissionMaintenanceRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int rejectPendingCommissionByOrderId(Long orderId) {
		return jdbcTemplate.update(REJECT_PENDING_BY_ORDER_SQL, orderId);
	}

	public int approvePendingCommissionAndAddBalanceByOrderId(Long orderId) {
		return jdbcTemplate.update(APPROVE_PENDING_AND_ADD_BALANCE_SQL, orderId);
	}
}
