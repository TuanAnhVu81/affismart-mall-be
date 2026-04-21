package com.affismart.mall.modules.order.service;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.common.enums.CommissionStatus;
import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.modules.affiliate.entity.AffiliateAccount;
import com.affismart.mall.modules.affiliate.entity.Commission;
import com.affismart.mall.modules.affiliate.repository.AffiliateAccountRepository;
import com.affismart.mall.modules.affiliate.repository.CommissionRepository;
import com.affismart.mall.modules.affiliate.repository.ReferralLinkRepository;
import com.affismart.mall.modules.order.entity.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionService {

	private static final Logger log = LoggerFactory.getLogger(CommissionService.class);
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final CommissionRepository commissionRepository;
	private final AffiliateAccountRepository affiliateAccountRepository;
	private final ReferralLinkRepository referralLinkRepository;

	public CommissionService(
			CommissionRepository commissionRepository,
			AffiliateAccountRepository affiliateAccountRepository,
			ReferralLinkRepository referralLinkRepository
	) {
		this.commissionRepository = commissionRepository;
		this.affiliateAccountRepository = affiliateAccountRepository;
		this.referralLinkRepository = referralLinkRepository;
	}

	@Transactional
	public void createPendingCommissionForPaidOrder(Order order) {
		if (order.getAffiliateAccountId() == null) {
			return;
		}
		if (!isPaidOrHigher(order.getStatus())) {
			log.warn(
					"Skip commission creation because order status is not paid or higher. order_id={}, status={}",
					order.getId(),
					order.getStatus()
			);
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

		if (commissionRepository.existsByOrder_Id(order.getId())) {
			log.info("Skipped commission creation for order_id={} (already exists)", order.getId());
			return;
		}

		AffiliateAccount affiliateAccount = affiliateAccountRepository.findByIdAndStatus(
				order.getAffiliateAccountId(),
				AffiliateAccountStatus.APPROVED
		).orElse(null);
		if (affiliateAccount == null) {
			log.info("Skipped commission creation for order_id={} (affiliate not approved)", order.getId());
			return;
		}

		BigDecimal rateSnapshot = affiliateAccount.getCommissionRate();
		BigDecimal commissionAmount = order.getTotalAmount()
				.multiply(rateSnapshot)
				.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
		if (commissionAmount.signum() <= 0) {
			log.warn(
					"Skip commission creation because calculated commission amount is invalid. order_id={}, amount={}",
					order.getId(),
					commissionAmount
			);
			return;
		}

		Commission commission = new Commission();
		commission.setAffiliateAccount(affiliateAccount);
		commission.setOrder(order);
		commission.setAmount(commissionAmount);
		commission.setRateSnapshot(rateSnapshot);
		commission.setStatus(CommissionStatus.PENDING);

		commissionRepository.save(commission);
		incrementReferralLinkConversions(order);
		log.info(
				"Created pending commission for paid order_id={} with rate_snapshot={} and amount={}",
				order.getId(),
				rateSnapshot,
				commissionAmount
		);
	}

	private void incrementReferralLinkConversions(Order order) {
		if (order.getReferralLinkId() == null) {
			return;
		}
		referralLinkRepository.incrementTotalConversionsById(order.getReferralLinkId());
	}

	private boolean isPaidOrHigher(OrderStatus status) {
		return status == OrderStatus.PAID
				|| status == OrderStatus.CONFIRMED
				|| status == OrderStatus.SHIPPED
				|| status == OrderStatus.DONE;
	}
}
