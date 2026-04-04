package com.affismart.mall.modules.user.dto.request;

import com.affismart.mall.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
		@NotNull(message = "Status is required")
		UserStatus status
) {
}
