package com.affismart.mall.modules.auth.dto.response;

import java.util.List;

public record AuthUserResponse(
		Long id,
		String email,
		String fullName,
		String status,
		List<String> roles
) {
}
