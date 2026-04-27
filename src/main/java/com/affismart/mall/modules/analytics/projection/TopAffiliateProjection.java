package com.affismart.mall.modules.analytics.projection;

import java.math.BigDecimal;

public interface TopAffiliateProjection {

	Long getAffiliateAccountId();

	Long getUserId();

	String getFullName();

	String getRefCode();

	long getConversionCount();

	BigDecimal getTotalCommission();

	BigDecimal getAttributedRevenue();
}
