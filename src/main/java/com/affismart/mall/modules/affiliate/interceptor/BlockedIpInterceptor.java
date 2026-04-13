package com.affismart.mall.modules.affiliate.interceptor;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.common.util.ClientIpResolver;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.affiliate.service.ClickTrackingRedisKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BlockedIpInterceptor implements HandlerInterceptor {

	private final StringRedisTemplate stringRedisTemplate;

	public BlockedIpInterceptor(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String clientIp = ClientIpResolver.resolve(request);
		String blockedIpKey = ClickTrackingRedisKeys.blockedIpKey(clientIp);
		if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(blockedIpKey))) {
			throw new AppException(ErrorCode.AFFILIATE_CLICK_RATE_LIMITED);
		}
		return true;
	}
}
