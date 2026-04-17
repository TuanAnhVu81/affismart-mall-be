package com.affismart.mall.modules.order.service;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.common.enums.CommissionStatus;
import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.modules.affiliate.entity.AffiliateAccount;
import com.affismart.mall.modules.affiliate.entity.Commission;
import com.affismart.mall.modules.affiliate.repository.AffiliateAccountRepository;
import com.affismart.mall.modules.affiliate.repository.CommissionRepository;
import com.affismart.mall.modules.order.entity.Order;
import java.math.BigDecimal;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommissionService Unit Tests")
class CommissionServiceTest {

	@Mock
	private CommissionRepository commissionRepository;

	@Mock
	private AffiliateAccountRepository affiliateAccountRepository;

	@InjectMocks
	private CommissionService commissionService;

	@Captor
	private ArgumentCaptor<Commission> commissionCaptor;

	// =========================================================
	// createPendingCommissionForPaidOrder()
	// =========================================================

	@Test
	@DisplayName("createPendingCommissionForPaidOrder: order without affiliate is ignored")
	void createPendingCommissionForPaidOrder_NoAffiliate_Ignored() {
		// Given
		Order order = createOrder(1L, null, OrderStatus.PAID, new BigDecimal("100.00"));

		// When
		commissionService.createPendingCommissionForPaidOrder(order);

		// Then
		verifyNoInteractions(commissionRepository, affiliateAccountRepository);
	}

	@Test
	@DisplayName("createPendingCommissionForPaidOrder: non-paid order is ignored")
	void createPendingCommissionForPaidOrder_NonPaidOrder_Ignored() {
		// Given
		Order order = createOrder(2L, 99L, OrderStatus.PENDING, new BigDecimal("100.00"));

		// When
		commissionService.createPendingCommissionForPaidOrder(order);

		// Then
		verifyNoInteractions(commissionRepository, affiliateAccountRepository);
	}

	@Test
	@DisplayName("createPendingCommissionForPaidOrder: existing commission is skipped")
	void createPendingCommissionForPaidOrder_ExistingCommission_Skipped() {
		// Given
		Order order = createOrder(3L, 99L, OrderStatus.PAID, new BigDecimal("250.00"));
		given(commissionRepository.existsByOrder_Id(3L)).willReturn(true);

		// When
		commissionService.createPendingCommissionForPaidOrder(order);

		// Then
		verify(commissionRepository).existsByOrder_Id(3L);
		verify(affiliateAccountRepository, never()).findByIdAndStatus(any(), any());
		verify(commissionRepository, never()).save(any());
	}

	@Test
	@DisplayName("createPendingCommissionForPaidOrder: approved affiliate snapshots rate and saves pending commission")
	void createPendingCommissionForPaidOrder_ApprovedAffiliate_SavesCommissionWithSnapshot() {
		// Given
		Order order = createOrder(4L, 88L, OrderStatus.PAID, new BigDecimal("250.00"));
		AffiliateAccount affiliateAccount = createAffiliateAccount(88L, new BigDecimal("12.50"));

		given(commissionRepository.existsByOrder_Id(4L)).willReturn(false);
		given(affiliateAccountRepository.findByIdAndStatus(88L, AffiliateAccountStatus.APPROVED))
				.willReturn(Optional.of(affiliateAccount));

		// When
		commissionService.createPendingCommissionForPaidOrder(order);

		// Then
		verify(commissionRepository).save(commissionCaptor.capture());
		Commission savedCommission = commissionCaptor.getValue();
		assertThat(savedCommission.getAffiliateAccount()).isEqualTo(affiliateAccount);
		assertThat(savedCommission.getOrder()).isEqualTo(order);
		assertThat(savedCommission.getRateSnapshot()).isEqualByComparingTo("12.50");
		assertThat(savedCommission.getAmount()).isEqualByComparingTo("31.25");
		assertThat(savedCommission.getStatus()).isEqualTo(CommissionStatus.PENDING);
	}

	@Test
	@DisplayName("createPendingCommissionForPaidOrder: non-approved affiliate does not create commission")
	void createPendingCommissionForPaidOrder_NonApprovedAffiliate_Ignored() {
		// Given
		Order order = createOrder(5L, 77L, OrderStatus.PAID, new BigDecimal("150.00"));
		given(commissionRepository.existsByOrder_Id(5L)).willReturn(false);
		given(affiliateAccountRepository.findByIdAndStatus(77L, AffiliateAccountStatus.APPROVED))
				.willReturn(Optional.empty());

		// When
		commissionService.createPendingCommissionForPaidOrder(order);

		// Then
		verify(commissionRepository, never()).save(any());
	}

	// =========================================================
	// Private Helper Methods
	// =========================================================

	private Order createOrder(Long orderId, Long affiliateAccountId, OrderStatus status, BigDecimal totalAmount) {
		Order order = new Order();
		order.setId(orderId);
		order.setAffiliateAccountId(affiliateAccountId);
		order.setStatus(status);
		order.setTotalAmount(totalAmount);
		return order;
	}

	private AffiliateAccount createAffiliateAccount(Long id, BigDecimal commissionRate) {
		AffiliateAccount affiliateAccount = new AffiliateAccount();
		affiliateAccount.setId(id);
		affiliateAccount.setStatus(AffiliateAccountStatus.APPROVED);
		affiliateAccount.setCommissionRate(commissionRate);
		return affiliateAccount;
	}
}
