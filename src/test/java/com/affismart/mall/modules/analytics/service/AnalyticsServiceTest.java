package com.affismart.mall.modules.analytics.service;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.common.enums.CommissionStatus;
import com.affismart.mall.common.enums.OrderStatus;
import com.affismart.mall.modules.analytics.dto.response.AnalyticsDashboardResponse;
import com.affismart.mall.modules.analytics.projection.DashboardOrderMetricsProjection;
import com.affismart.mall.modules.analytics.projection.TopAffiliateProjection;
import com.affismart.mall.modules.analytics.projection.TopProductProjection;
import com.affismart.mall.modules.analytics.repository.AnalyticsAffiliateRepository;
import com.affismart.mall.modules.analytics.repository.AnalyticsCommissionRepository;
import com.affismart.mall.modules.analytics.repository.AnalyticsOrderItemRepository;
import com.affismart.mall.modules.analytics.repository.AnalyticsOrderRepository;
import com.affismart.mall.modules.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Unit Tests")
class AnalyticsServiceTest {

	@Mock
	private AnalyticsOrderRepository analyticsOrderRepository;

	@Mock
	private AnalyticsOrderItemRepository analyticsOrderItemRepository;

	@Mock
	private AnalyticsAffiliateRepository analyticsAffiliateRepository;

	@Mock
	private AnalyticsCommissionRepository analyticsCommissionRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private AnalyticsService analyticsService;

	@Captor
	private ArgumentCaptor<Pageable> pageableCaptor;

	@Captor
	private ArgumentCaptor<Collection<CommissionStatus>> commissionStatusesCaptor;

	@Test
	@DisplayName("getDashboard: Happy Path - returns aggregated admin metrics")
	void getDashboard_ExistingData_ReturnsAggregatedMetrics() {
		DashboardOrderMetricsProjection orderMetrics = dashboardOrderMetrics(new BigDecimal("1500000.00"), 7);
		given(analyticsOrderRepository.getOrderMetricsByStatus(OrderStatus.DONE)).willReturn(orderMetrics);
		given(analyticsOrderRepository.count()).willReturn(12L);
		given(userRepository.count()).willReturn(34L);
		given(analyticsAffiliateRepository.countByStatus(AffiliateAccountStatus.APPROVED)).willReturn(5L);

		AnalyticsDashboardResponse result = analyticsService.getDashboard();

		assertThat(result.grossMerchandiseValue()).isEqualByComparingTo("1500000.00");
		assertThat(result.completedOrders()).isEqualTo(7);
		assertThat(result.totalOrders()).isEqualTo(12);
		assertThat(result.totalUsers()).isEqualTo(34);
		assertThat(result.activeAffiliates()).isEqualTo(5);
	}

	@Test
	@DisplayName("getDashboard: Edge Case - null GMV is normalized to zero")
	void getDashboard_NullGrossMerchandiseValue_ReturnsZeroGmv() {
		given(analyticsOrderRepository.getOrderMetricsByStatus(OrderStatus.DONE))
				.willReturn(dashboardOrderMetrics(null, 0));

		AnalyticsDashboardResponse result = analyticsService.getDashboard();

		assertThat(result.grossMerchandiseValue()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	@DisplayName("getTopProducts: Happy Path - queries DONE order items with requested limit")
	void getTopProducts_ValidLimit_QueriesDoneOrdersWithLimit() {
		List<TopProductProjection> expected = List.of(topProduct(1L, "Keyboard", 10, new BigDecimal("500000.00")));
		given(analyticsOrderItemRepository.findTopProducts(org.mockito.ArgumentMatchers.eq(OrderStatus.DONE), org.mockito.ArgumentMatchers.any(Pageable.class)))
				.willReturn(expected);

		List<TopProductProjection> result = analyticsService.getTopProducts(8);

		assertThat(result).isEqualTo(expected);
		verify(analyticsOrderItemRepository).findTopProducts(org.mockito.ArgumentMatchers.eq(OrderStatus.DONE), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(8);
	}

	@Test
	@DisplayName("getTopProducts: Edge Case - limit is capped at 50")
	void getTopProducts_LimitTooLarge_CapsLimitAtFifty() {
		given(analyticsOrderItemRepository.findTopProducts(org.mockito.ArgumentMatchers.eq(OrderStatus.DONE), org.mockito.ArgumentMatchers.any(Pageable.class)))
				.willReturn(List.of());

		analyticsService.getTopProducts(999);

		verify(analyticsOrderItemRepository).findTopProducts(org.mockito.ArgumentMatchers.eq(OrderStatus.DONE), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
	}

	@Test
	@DisplayName("getTopAffiliates: Happy Path - queries earned commission statuses with default limit")
	void getTopAffiliates_NullLimit_QueriesEarnedStatusesWithDefaultLimit() {
		List<TopAffiliateProjection> expected = List.of(topAffiliate(10L, "Alice", new BigDecimal("2500000.00")));
		given(analyticsCommissionRepository.findTopAffiliates(org.mockito.ArgumentMatchers.anyCollection(), org.mockito.ArgumentMatchers.any(Pageable.class)))
				.willReturn(expected);

		List<TopAffiliateProjection> result = analyticsService.getTopAffiliates(null);

		assertThat(result).isEqualTo(expected);
		verify(analyticsCommissionRepository).findTopAffiliates(commissionStatusesCaptor.capture(), pageableCaptor.capture());
		assertThat(commissionStatusesCaptor.getValue())
				.containsExactlyInAnyOrder(CommissionStatus.APPROVED, CommissionStatus.PAID);
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
	}

	private DashboardOrderMetricsProjection dashboardOrderMetrics(BigDecimal grossMerchandiseValue, long completedOrders) {
		return new DashboardOrderMetricsProjection() {
			@Override
			public BigDecimal getGrossMerchandiseValue() {
				return grossMerchandiseValue;
			}

			@Override
			public long getCompletedOrders() {
				return completedOrders;
			}
		};
	}

	private TopProductProjection topProduct(Long productId, String productName, long quantitySold, BigDecimal revenue) {
		return new TopProductProjection() {
			@Override
			public Long getProductId() {
				return productId;
			}

			@Override
			public String getProductName() {
				return productName;
			}

			@Override
			public String getSku() {
				return "SKU-" + productId;
			}

			@Override
			public String getImageUrl() {
				return null;
			}

			@Override
			public long getQuantitySold() {
				return quantitySold;
			}

			@Override
			public BigDecimal getRevenue() {
				return revenue;
			}
		};
	}

	private TopAffiliateProjection topAffiliate(Long affiliateAccountId, String fullName, BigDecimal attributedRevenue) {
		return new TopAffiliateProjection() {
			@Override
			public Long getAffiliateAccountId() {
				return affiliateAccountId;
			}

			@Override
			public Long getUserId() {
				return 100L + affiliateAccountId;
			}

			@Override
			public String getFullName() {
				return fullName;
			}

			@Override
			public String getRefCode() {
				return "AFF" + affiliateAccountId;
			}

			@Override
			public long getConversionCount() {
				return 3;
			}

			@Override
			public BigDecimal getTotalCommission() {
				return new BigDecimal("250000.00");
			}

			@Override
			public BigDecimal getAttributedRevenue() {
				return attributedRevenue;
			}
		};
	}
}
