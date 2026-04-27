package com.affismart.mall.modules.analytics.projection;

import java.math.BigDecimal;

public interface DashboardOrderMetricsProjection {

	BigDecimal getGrossMerchandiseValue();

	long getCompletedOrders();
}
