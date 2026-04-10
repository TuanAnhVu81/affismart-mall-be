package com.affismart.mall.modules.order.service;

import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.repository.CommissionMaintenanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionService {

	private static final Logger log = LoggerFactory.getLogger(CommissionService.class);

	private final CommissionMaintenanceRepository commissionMaintenanceRepository;

	public CommissionService(CommissionMaintenanceRepository commissionMaintenanceRepository) {
		this.commissionMaintenanceRepository = commissionMaintenanceRepository;
	}

	@Transactional
	public void createPendingCommissionForPaidOrder(Order order) {
		if (order.getAffiliateAccountId() == null) {
			return;
		}
		if (order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0) {
			log.warn(
					"Skip commission creation because order total is invalid. order_id={}, total_amount={}",
					order.getId(),
					order.getTotalAmount()
			);
			return;
		}

		int inserted = commissionMaintenanceRepository.insertPendingCommission(
				order.getAffiliateAccountId(),
				order.getId(),
				order.getTotalAmount()
		);
		if (inserted > 0) {
			log.info("Created pending commission for paid order_id={}", order.getId());
		} else {
			log.info("Skipped commission creation for order_id={} (already exists or affiliate not approved)", order.getId());
		}
	}
}
