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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PayoutService Unit Tests")
class PayoutServiceTest {

	@Mock
	private AffiliateAccountRepository affiliateAccountRepository;

	@Mock
	private CommissionRepository commissionRepository;

	@Mock
	private PayoutRequestRepository payoutRequestRepository;

	@Mock
	private AffiliateMapper affiliateMapper;

	@InjectMocks
	private PayoutService payoutService;

	@Captor
	private ArgumentCaptor<AffiliateAccount> affiliateAccountCaptor;

	@Captor
	private ArgumentCaptor<List<Commission>> commissionListCaptor;

	// =========================================================
	// createMyPayoutRequest()
	// =========================================================

	@Test
	@DisplayName("createMyPayoutRequest: approved commissions are linked and balance is deducted")
	void createMyPayoutRequest_ValidFlow_CreatesPayoutAndDeductsBalance() {
		// Given
		Long userId = 10L;
		AffiliateAccount account = createAffiliateAccount(1L, AffiliateAccountStatus.APPROVED, new BigDecimal("500000"));
		List<Commission> commissions = List.of(
				createCommission(100L, account, new BigDecimal("120000"), CommissionStatus.APPROVED),
				createCommission(101L, account, new BigDecimal("130000"), CommissionStatus.APPROVED)
		);

		PayoutRequest savedPayoutRequest = new PayoutRequest();
		savedPayoutRequest.setId(1000L);
		savedPayoutRequest.setAffiliateAccount(account);
		savedPayoutRequest.setAmount(new BigDecimal("250000"));
		savedPayoutRequest.setStatus(PayoutRequestStatus.PENDING);
		PayoutRequestResponse expectedResponse = new PayoutRequestResponse(
				1000L,
				1L,
				new BigDecimal("250000"),
				"PENDING",
				null,
				null,
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		given(affiliateAccountRepository.findByUser_Id(userId)).willReturn(Optional.of(account));
		given(affiliateAccountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(account));
		given(commissionRepository.findEligibleCommissionsForPayoutForUpdate(1L, CommissionStatus.APPROVED))
				.willReturn(commissions);
		given(payoutRequestRepository.save(any(PayoutRequest.class))).willReturn(savedPayoutRequest);
		given(affiliateMapper.toPayoutRequestResponse(savedPayoutRequest)).willReturn(expectedResponse);

		// When
		PayoutRequestResponse actual = payoutService.createMyPayoutRequest(userId);

		// Then
		verify(commissionRepository).saveAll(commissionListCaptor.capture());
		assertThat(commissionListCaptor.getValue()).allMatch(c -> c.getPayoutRequest() == savedPayoutRequest);

		verify(affiliateAccountRepository).save(affiliateAccountCaptor.capture());
		assertThat(affiliateAccountCaptor.getValue().getBalance()).isEqualByComparingTo("250000");
		assertThat(actual).isEqualTo(expectedResponse);
	}

	@Test
	@DisplayName("createMyPayoutRequest: total approved commissions below minimum throws PAYOUT_AMOUNT_BELOW_MINIMUM")
	void createMyPayoutRequest_BelowMinimum_ThrowsError() {
		// Given
		Long userId = 11L;
		AffiliateAccount account = createAffiliateAccount(2L, AffiliateAccountStatus.APPROVED, new BigDecimal("150000"));
		List<Commission> commissions = List.of(
				createCommission(102L, account, new BigDecimal("80000"), CommissionStatus.APPROVED),
				createCommission(103L, account, new BigDecimal("70000"), CommissionStatus.APPROVED)
		);

		given(affiliateAccountRepository.findByUser_Id(userId)).willReturn(Optional.of(account));
		given(affiliateAccountRepository.findByIdForUpdate(2L)).willReturn(Optional.of(account));
		given(commissionRepository.findEligibleCommissionsForPayoutForUpdate(2L, CommissionStatus.APPROVED))
				.willReturn(commissions);

		// When + Then
		assertThatThrownBy(() -> payoutService.createMyPayoutRequest(userId))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PAYOUT_AMOUNT_BELOW_MINIMUM);

		verify(payoutRequestRepository, never()).save(any());
		verify(affiliateAccountRepository, never()).save(any(AffiliateAccount.class));
	}

	// =========================================================
	// updatePayoutStatus()
	// =========================================================

	@Test
	@DisplayName("updatePayoutStatus: TRANSFERRED marks linked commissions as PAID")
	void updatePayoutStatus_Transferred_MarksCommissionsPaid() {
		// Given
		PayoutRequest payoutRequest = createPayoutRequest(200L, PayoutRequestStatus.PENDING, new BigDecimal("250000"));
		List<Commission> linkedCommissions = List.of(
				createCommission(104L, payoutRequest.getAffiliateAccount(), new BigDecimal("120000"), CommissionStatus.APPROVED),
				createCommission(105L, payoutRequest.getAffiliateAccount(), new BigDecimal("130000"), CommissionStatus.APPROVED)
		);
		linkedCommissions.forEach(commission -> commission.setPayoutRequest(payoutRequest));

		PayoutRequestResponse expectedResponse = new PayoutRequestResponse(
				200L,
				1L,
				new BigDecimal("250000"),
				"TRANSFERRED",
				"Banked",
				LocalDateTime.now(),
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		given(payoutRequestRepository.findByIdForUpdate(200L)).willReturn(Optional.of(payoutRequest));
		given(commissionRepository.findByPayoutRequestIdForUpdate(200L)).willReturn(linkedCommissions);
		given(payoutRequestRepository.save(any(PayoutRequest.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(affiliateMapper.toPayoutRequestResponse(any(PayoutRequest.class))).willReturn(expectedResponse);

		// When
		payoutService.updatePayoutStatus(200L, new UpdatePayoutRequestStatusRequest(PayoutRequestStatus.TRANSFERRED, "Banked"));

		// Then
		assertThat(linkedCommissions).allMatch(c -> c.getStatus() == CommissionStatus.PAID);
		verify(commissionRepository).saveAll(linkedCommissions);
		verify(affiliateAccountRepository, never()).save(any(AffiliateAccount.class));
	}

	@Test
	@DisplayName("updatePayoutStatus: REJECTED refunds affiliate balance and unlinks commissions")
	void updatePayoutStatus_Rejected_RefundsAndUnlinks() {
		// Given
		AffiliateAccount account = createAffiliateAccount(1L, AffiliateAccountStatus.APPROVED, new BigDecimal("0"));
		PayoutRequest payoutRequest = createPayoutRequest(201L, PayoutRequestStatus.PENDING, new BigDecimal("250000"));
		payoutRequest.setAffiliateAccount(account);

		List<Commission> linkedCommissions = List.of(
				createCommission(106L, account, new BigDecimal("120000"), CommissionStatus.APPROVED),
				createCommission(107L, account, new BigDecimal("130000"), CommissionStatus.APPROVED)
		);
		linkedCommissions.forEach(commission -> commission.setPayoutRequest(payoutRequest));

		PayoutRequestResponse expectedResponse = new PayoutRequestResponse(
				201L,
				1L,
				new BigDecimal("250000"),
				"REJECTED",
				"Fraud suspicion",
				LocalDateTime.now(),
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		given(payoutRequestRepository.findByIdForUpdate(201L)).willReturn(Optional.of(payoutRequest));
		given(affiliateAccountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(account));
		given(commissionRepository.findByPayoutRequestIdForUpdate(201L)).willReturn(linkedCommissions);
		given(payoutRequestRepository.save(any(PayoutRequest.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(affiliateMapper.toPayoutRequestResponse(any(PayoutRequest.class))).willReturn(expectedResponse);

		// When
		payoutService.updatePayoutStatus(201L, new UpdatePayoutRequestStatusRequest(PayoutRequestStatus.REJECTED, "Fraud suspicion"));

		// Then
		assertThat(account.getBalance()).isEqualByComparingTo("250000");
		assertThat(linkedCommissions).allMatch(c -> c.getPayoutRequest() == null && c.getStatus() == CommissionStatus.APPROVED);
		verify(affiliateAccountRepository).save(account);
		verify(commissionRepository).saveAll(linkedCommissions);
	}

	@Test
	@DisplayName("updatePayoutStatus: invalid transition throws PAYOUT_STATUS_TRANSITION_NOT_ALLOWED")
	void updatePayoutStatus_InvalidTransition_ThrowsError() {
		// Given
		PayoutRequest transferredRequest = createPayoutRequest(202L, PayoutRequestStatus.TRANSFERRED, new BigDecimal("250000"));
		given(payoutRequestRepository.findByIdForUpdate(202L)).willReturn(Optional.of(transferredRequest));

		// When + Then
		assertThatThrownBy(() -> payoutService.updatePayoutStatus(
				202L,
				new UpdatePayoutRequestStatusRequest(PayoutRequestStatus.REJECTED, "Cannot reject transferred")
		))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PAYOUT_STATUS_TRANSITION_NOT_ALLOWED);

		verifyNoInteractions(commissionRepository);
	}

	// =========================================================
	// Private Helper Methods
	// =========================================================

	private AffiliateAccount createAffiliateAccount(Long id, AffiliateAccountStatus status, BigDecimal balance) {
		AffiliateAccount account = new AffiliateAccount();
		account.setId(id);
		account.setStatus(status);
		account.setBalance(balance);
		account.setCommissionRate(new BigDecimal("10.00"));
		return account;
	}

	private Commission createCommission(Long id, AffiliateAccount account, BigDecimal amount, CommissionStatus status) {
		Commission commission = new Commission();
		commission.setId(id);
		commission.setAffiliateAccount(account);
		commission.setAmount(amount);
		commission.setStatus(status);
		commission.setRateSnapshot(new BigDecimal("10.00"));
		return commission;
	}

	private PayoutRequest createPayoutRequest(Long id, PayoutRequestStatus status, BigDecimal amount) {
		PayoutRequest payoutRequest = new PayoutRequest();
		payoutRequest.setId(id);
		payoutRequest.setStatus(status);
		payoutRequest.setAmount(amount);
		payoutRequest.setAffiliateAccount(createAffiliateAccount(1L, AffiliateAccountStatus.APPROVED, BigDecimal.ZERO));
		return payoutRequest;
	}
}
