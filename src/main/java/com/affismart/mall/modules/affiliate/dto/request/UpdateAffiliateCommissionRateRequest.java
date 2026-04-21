package com.affismart.mall.modules.affiliate.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateAffiliateCommissionRateRequest(
		@NotNull(message = "Commission rate is required")
		@DecimalMin(value = "0.00", inclusive = true, message = "Commission rate must be at least 0")
		@DecimalMax(value = "100.00", inclusive = true, message = "Commission rate must be at most 100")
		@Digits(integer = 3, fraction = 2, message = "Commission rate must have up to 2 decimal places")
		BigDecimal commissionRate
) {
}
