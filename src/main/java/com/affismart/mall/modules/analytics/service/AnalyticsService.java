package com.affismart.mall.modules.analytics.service;

import com.affismart.mall.config.CacheConfig;
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
import java.util.List;
import java.util.Set;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

	private static final int DEFAULT_LIMIT = 10;
	private static final int MAX_LIMIT = 50;
	private static final Set<CommissionStatus> EARNED_COMMISSION_STATUSES = Set.of(
			CommissionStatus.APPROVED,
			CommissionStatus.PAID
	);

	private final AnalyticsOrderRepository analyticsOrderRepository;
	private final AnalyticsOrderItemRepository analyticsOrderItemRepository;
	private final AnalyticsAffiliateRepository analyticsAffiliateRepository;
	private final AnalyticsCommissionRepository analyticsCommissionRepository;
	private final UserRepository userRepository;

	public AnalyticsService(
			AnalyticsOrderRepository analyticsOrderRepository,
			AnalyticsOrderItemRepository analyticsOrderItemRepository,
			AnalyticsAffiliateRepository analyticsAffiliateRepository,
			AnalyticsCommissionRepository analyticsCommissionRepository,
			UserRepository userRepository
	) {
		this.analyticsOrderRepository = analyticsOrderRepository;
		this.analyticsOrderItemRepository = analyticsOrderItemRepository;
		this.analyticsAffiliateRepository = analyticsAffiliateRepository;
		this.analyticsCommissionRepository = analyticsCommissionRepository;
		this.userRepository = userRepository;
	}

	@Cacheable(cacheNames = CacheConfig.ANALYTICS_DASHBOARD_CACHE)
	public AnalyticsDashboardResponse getDashboard() {
		DashboardOrderMetricsProjection orderMetrics = analyticsOrderRepository.getOrderMetricsByStatus(OrderStatus.DONE);

		return new AnalyticsDashboardResponse(
				defaultIfNull(orderMetrics.getGrossMerchandiseValue()),
				analyticsOrderRepository.count(),
				orderMetrics.getCompletedOrders(),
				userRepository.count(),
				analyticsAffiliateRepository.countByStatus(AffiliateAccountStatus.APPROVED)
		);
	}

	@Cacheable(
			cacheNames = CacheConfig.ANALYTICS_TOP_PRODUCTS_CACHE,
			key = "#limit == null ? 10 : T(java.lang.Math).min(T(java.lang.Math).max(#limit, 1), 50)"
	)
	public List<TopProductProjection> getTopProducts(Integer limit) {
		return analyticsOrderItemRepository.findTopProducts(
				OrderStatus.DONE,
				PageRequest.of(0, normalizeLimit(limit))
		);
	}

	@Cacheable(
			cacheNames = CacheConfig.ANALYTICS_TOP_AFFILIATES_CACHE,
			key = "#limit == null ? 10 : T(java.lang.Math).min(T(java.lang.Math).max(#limit, 1), 50)"
	)
	public List<TopAffiliateProjection> getTopAffiliates(Integer limit) {
		return analyticsCommissionRepository.findTopAffiliates(
				EARNED_COMMISSION_STATUSES,
				PageRequest.of(0, normalizeLimit(limit))
		);
	}

	private int normalizeLimit(Integer limit) {
		if (limit == null) {
			return DEFAULT_LIMIT;
		}
		return Math.min(Math.max(limit, 1), MAX_LIMIT);
	}

	private BigDecimal defaultIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
