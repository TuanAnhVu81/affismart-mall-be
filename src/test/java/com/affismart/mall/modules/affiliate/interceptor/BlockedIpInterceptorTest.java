package com.affismart.mall.modules.affiliate.interceptor;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.affiliate.service.ClickTrackingRedisKeys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockedIpInterceptor Unit Tests")
class BlockedIpInterceptorTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@InjectMocks
	private BlockedIpInterceptor blockedIpInterceptor;

	// =========================================================
	// preHandle()
	// =========================================================

	@Test
	@DisplayName("preHandle: non-blocked IP is allowed")
	void preHandle_NotBlockedIp_ReturnsTrue() throws Exception {
		// Given
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/affiliate/track-click");
		request.setRemoteAddr("203.0.113.11");
		MockHttpServletResponse response = new MockHttpServletResponse();
		String blockedIpKey = ClickTrackingRedisKeys.blockedIpKey("203.0.113.11");

		given(stringRedisTemplate.hasKey(blockedIpKey)).willReturn(false);

		// When
		boolean allowed = blockedIpInterceptor.preHandle(request, response, new Object());

		// Then
		assertThat(allowed).isTrue();
		verify(stringRedisTemplate).hasKey(blockedIpKey);
	}

	@Test
	@DisplayName("preHandle: blocked IP throws AFFILIATE_CLICK_RATE_LIMITED")
	void preHandle_BlockedIp_ThrowsRateLimited() {
		// Given
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/affiliate/track-click");
		request.addHeader("X-Forwarded-For", "198.51.100.90, 10.0.0.5");
		MockHttpServletResponse response = new MockHttpServletResponse();
		String blockedIpKey = ClickTrackingRedisKeys.blockedIpKey("198.51.100.90");

		given(stringRedisTemplate.hasKey(blockedIpKey)).willReturn(true);

		// When + Then
		assertThatThrownBy(() -> blockedIpInterceptor.preHandle(request, response, new Object()))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AFFILIATE_CLICK_RATE_LIMITED);

		verify(stringRedisTemplate).hasKey(blockedIpKey);
	}
}
