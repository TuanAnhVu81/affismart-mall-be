package com.affismart.mall.modules.affiliate.service;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.common.enums.CommissionStatus;
import com.affismart.mall.common.enums.PayoutRequestStatus;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.affiliate.dto.request.UpdatePayoutRequestStatusRequest;
import com.affismart.mall.modules.affiliate.dto.response.PayoutRequestResponse;
import com.affismart.mall.modules.affiliate.entity.AffiliateAccount;
import com.affismart.mall.modules.affiliate.entity.Commission;
import com.affismart.mall.modules.affiliate.entity.PayoutRequest;
import com.affismart.mall.modules.affiliate.mapper.AffiliateMapper;
import com.affismart.mall.modules.affiliate.repository.AffiliateAccountRepository;
import com.affismart.mall.modules.affiliate.repository.CommissionRepository;
import com.affismart.mall.modules.affiliate.repository.PayoutRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PayoutService {

	private static final BigDecimal MIN_PAYOUT_AMOUNT = new BigDecimal("200000");

	private final AffiliateAccountRepository affiliateAccountRepository;
	private final CommissionRepository commissionRepository;
	private final PayoutRequestRepository payoutRequestRepository;
	private final AffiliateMapper affiliateMapper;

	public PayoutService(
			AffiliateAccountRepository affiliateAccountRepository,
			CommissionRepository commissionRepository,
			PayoutRequestRepository payoutRequestRepository,
			AffiliateMapper affiliateMapper
	) {
		this.affiliateAccountRepository = affiliateAccountRepository;
		this.commissionRepository = commissionRepository;
		this.payoutRequestRepository = payoutRequestRepository;
		this.affiliateMapper = affiliateMapper;
	}

	@Transactional
	public PayoutRequestResponse createMyPayoutRequest(Long userId) {
		AffiliateAccount affiliateAccount = getApprovedAffiliateAccountByUser(userId);
		AffiliateAccount lockedAffiliateAccount = lockAffiliateAccount(affiliateAccount.getId());

		List<Commission> eligibleCommissions = commissionRepository.findEligibleCommissionsForPayoutForUpdate(
				lockedAffiliateAccount.getId(),
				CommissionStatus.APPROVED
		);
		if (eligibleCommissions.isEmpty()) {
			throw new AppException(ErrorCode.PAYOUT_AMOUNT_BELOW_MINIMUM, "No approved commissions available for payout");
		}

		BigDecimal payoutAmount = eligibleCommissions.stream()
				.map(Commission::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		if (payoutAmount.compareTo(MIN_PAYOUT_AMOUNT) < 0) {
			throw new AppException(
					ErrorCode.PAYOUT_AMOUNT_BELOW_MINIMUM,
					"Payout amount must be at least " + MIN_PAYOUT_AMOUNT.toPlainString()
			);
		}

		if (lockedAffiliateAccount.getBalance().compareTo(payoutAmount) < 0) {
			throw new AppException(ErrorCode.PAYOUT_BALANCE_INSUFFICIENT);
		}

		PayoutRequest payoutRequest = new PayoutRequest();
		payoutRequest.setAffiliateAccount(lockedAffiliateAccount);
		payoutRequest.setAmount(payoutAmount);
		payoutRequest.setStatus(PayoutRequestStatus.PENDING);
		PayoutRequest savedPayoutRequest = payoutRequestRepository.save(payoutRequest);

		for (Commission commission : eligibleCommissions) {
			commission.setPayoutRequest(savedPayoutRequest);
		}
		commissionRepository.saveAll(eligibleCommissions);

		lockedAffiliateAccount.setBalance(lockedAffiliateAccount.getBalance().subtract(payoutAmount));
		affiliateAccountRepository.save(lockedAffiliateAccount);

		return affiliateMapper.toPayoutRequestResponse(savedPayoutRequest);
	}

	@Transactional
	public PayoutRequestResponse updatePayoutStatus(Long payoutRequestId, UpdatePayoutRequestStatusRequest request) {
		PayoutRequest payoutRequest = payoutRequestRepository.findByIdForUpdate(payoutRequestId)
				.orElseThrow(() -> new AppException(ErrorCode.PAYOUT_REQUEST_NOT_FOUND));

		PayoutRequestStatus targetStatus = request.status();
		validateStatusTransition(payoutRequest.getStatus(), targetStatus);

		if (targetStatus == PayoutRequestStatus.TRANSFERRED) {
			markLinkedCommissionsPaid(payoutRequest.getId());
			payoutRequest.setResolvedAt(LocalDateTime.now());
		} else if (targetStatus == PayoutRequestStatus.REJECTED) {
			refundBalanceAndUnlinkCommissions(payoutRequest);
			payoutRequest.setResolvedAt(LocalDateTime.now());
		}

		payoutRequest.setStatus(targetStatus);
		payoutRequest.setAdminNote(normalizeNote(request.adminNote()));
		PayoutRequest savedPayoutRequest = payoutRequestRepository.save(payoutRequest);
		return affiliateMapper.toPayoutRequestResponse(savedPayoutRequest);
	}

	private AffiliateAccount getApprovedAffiliateAccountByUser(Long userId) {
		AffiliateAccount account = affiliateAccountRepository.findByUser_Id(userId)
				.orElseThrow(() -> new AppException(ErrorCode.AFFILIATE_ACCOUNT_NOT_FOUND));
		if (account.getStatus() != AffiliateAccountStatus.APPROVED) {
			throw new AppException(ErrorCode.AFFILIATE_ACCOUNT_NOT_APPROVED);
		}
		return account;
	}

	private AffiliateAccount lockAffiliateAccount(Long affiliateAccountId) {
		AffiliateAccount lockedAccount = affiliateAccountRepository.findByIdForUpdate(affiliateAccountId)
				.orElseThrow(() -> new AppException(ErrorCode.AFFILIATE_ACCOUNT_NOT_FOUND));
		if (lockedAccount.getStatus() != AffiliateAccountStatus.APPROVED) {
			throw new AppException(ErrorCode.AFFILIATE_ACCOUNT_NOT_APPROVED);
		}
		return lockedAccount;
	}

	private void markLinkedCommissionsPaid(Long payoutRequestId) {
		List<Commission> commissions = commissionRepository.findByPayoutRequestIdForUpdate(payoutRequestId);
		for (Commission commission : commissions) {
			if (commission.getStatus() == CommissionStatus.APPROVED) {
				commission.setStatus(CommissionStatus.PAID);
			}
		}
		commissionRepository.saveAll(commissions);
	}

	private void refundBalanceAndUnlinkCommissions(PayoutRequest payoutRequest) {
		AffiliateAccount lockedAffiliateAccount = lockAffiliateAccount(payoutRequest.getAffiliateAccount().getId());
		lockedAffiliateAccount.setBalance(lockedAffiliateAccount.getBalance().add(payoutRequest.getAmount()));
		affiliateAccountRepository.save(lockedAffiliateAccount);

		List<Commission> commissions = commissionRepository.findByPayoutRequestIdForUpdate(payoutRequest.getId());
		for (Commission commission : commissions) {
			commission.setPayoutRequest(null);
			if (commission.getStatus() != CommissionStatus.PAID) {
				commission.setStatus(CommissionStatus.APPROVED);
			}
		}
		commissionRepository.saveAll(commissions);
	}

	private void validateStatusTransition(PayoutRequestStatus currentStatus, PayoutRequestStatus targetStatus) {
		if (targetStatus == null || targetStatus == PayoutRequestStatus.PENDING) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Payout status must be APPROVED, TRANSFERRED, or REJECTED");
		}
		if (currentStatus == targetStatus) {
			return;
		}

		boolean allowed = switch (currentStatus) {
			case PENDING -> targetStatus == PayoutRequestStatus.APPROVED
					|| targetStatus == PayoutRequestStatus.TRANSFERRED
					|| targetStatus == PayoutRequestStatus.REJECTED;
			case APPROVED -> targetStatus == PayoutRequestStatus.TRANSFERRED
					|| targetStatus == PayoutRequestStatus.REJECTED;
			case TRANSFERRED, REJECTED -> false;
		};

		if (!allowed) {
			throw new AppException(
					ErrorCode.PAYOUT_STATUS_TRANSITION_NOT_ALLOWED,
					"Cannot transition payout status from " + currentStatus + " to " + targetStatus
			);
		}
	}

	private String normalizeNote(String adminNote) {
		if (!StringUtils.hasText(adminNote)) {
			return null;
		}
		return adminNote.trim();
	}
}
