package com.affismart.mall.modules.analytics.dto.response;

import java.math.BigDecimal;

public record AnalyticsDashboardResponse(
		BigDecimal grossMerchandiseValue,
		long totalOrders,
		long completedOrders,
		long totalUsers,
		long activeAffiliates
) {
}
