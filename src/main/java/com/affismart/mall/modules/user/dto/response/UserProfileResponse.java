package com.affismart.mall.modules.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileResponse(
		Long id,
		String email,
		String fullName,
		String phone,
		String defaultShippingAddress,
		String status,
		List<String> roles,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
