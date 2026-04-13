package com.affismart.mall.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientIpResolver {

	private ClientIpResolver() {
	}

	public static String resolve(HttpServletRequest request) {
		if (request == null) {
			return "unknown";
		}

		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(forwardedFor)) {
			String firstIp = forwardedFor.split(",")[0].trim();
			if (StringUtils.hasText(firstIp)) {
				return firstIp;
			}
		}

		String realIp = request.getHeader("X-Real-IP");
		if (StringUtils.hasText(realIp)) {
			return realIp.trim();
		}

		String remoteAddr = request.getRemoteAddr();
		if (StringUtils.hasText(remoteAddr)) {
			return remoteAddr.trim();
		}

		return "unknown";
	}
}
