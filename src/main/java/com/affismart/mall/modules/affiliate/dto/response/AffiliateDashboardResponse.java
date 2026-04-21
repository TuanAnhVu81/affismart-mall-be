package com.affismart.mall.modules.affiliate.dto.response;

import java.math.BigDecimal;

public record AffiliateDashboardResponse(
		Long totalClicks,
		Long totalConversions,
		BigDecimal conversionRate,
		BigDecimal balance,
		BigDecimal totalCommissionEarned
) {
}
