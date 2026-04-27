package com.affismart.mall.modules.analytics.projection;

import java.math.BigDecimal;

public interface TopProductProjection {

	Long getProductId();

	String getProductName();

	String getSku();

	String getImageUrl();

	long getQuantitySold();

	BigDecimal getRevenue();
}
