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

	private final JdbcTemplate jdbcTemplate;

	public CommissionMaintenanceRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int rejectPendingCommissionByOrderId(Long orderId) {
		return jdbcTemplate.update(REJECT_PENDING_BY_ORDER_SQL, orderId);
	}
}
