package com.affismart.mall.modules.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserSummaryResponse(
		Long id,
		String email,
		String fullName,
		String phone,
		String status,
		List<String> roles,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
