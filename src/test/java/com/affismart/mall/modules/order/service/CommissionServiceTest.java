package com.affismart.mall.modules.order.service;

import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.repository.CommissionMaintenanceRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommissionService Unit Tests")
class CommissionServiceTest {

	@Mock
	private CommissionMaintenanceRepository commissionMaintenanceRepository;

	@InjectMocks
	private CommissionService commissionService;

	@Test
	@DisplayName("createPendingCommissionForPaidOrder: order without affiliate is ignored")
	void createPendingCommissionForPaidOrder_NoAffiliate_Ignored() {
		// Given
		Order order = createOrder(1L, null, new BigDecimal("100.00"));

		// When
		commissionService.createPendingCommissionForPaidOrder(order);

		// Then
		verify(commissionMaintenanceRepository, never()).insertPendingCommission(any(), any(), any());
	}

	@Test
	@DisplayName("createPendingCommissionForPaidOrder: valid affiliate inserts pending commission")
	void createPendingCommissionForPaidOrder_ValidAffiliate_InsertsPendingCommission() {
		// Given
		Order order = createOrder(2L, 99L, new BigDecimal("250.00"));
		given(commissionMaintenanceRepository.insertPendingCommission(99L, 2L, new BigDecimal("250.00"))).willReturn(1);

		// When
		commissionService.createPendingCommissionForPaidOrder(order);

		// Then
		verify(commissionMaintenanceRepository).insertPendingCommission(99L, 2L, new BigDecimal("250.00"));
	}

	private Order createOrder(Long orderId, Long affiliateAccountId, BigDecimal totalAmount) {
		Order order = new Order();
		order.setId(orderId);
		order.setAffiliateAccountId(affiliateAccountId);
		order.setTotalAmount(totalAmount);
		return order;
	}
}
