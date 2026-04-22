package com.affismart.mall.modules.affiliate.service;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.affiliate.config.AffiliateClickTrackingProperties;
import com.affismart.mall.modules.affiliate.dto.response.BlockedIpResponse;
import com.affismart.mall.modules.affiliate.entity.BlockedClickLog;
import com.affismart.mall.modules.affiliate.repository.BlockedClickLogRepository;
import com.affismart.mall.modules.affiliate.repository.ReferralLinkRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClickTrackingService Unit Tests")
class ClickTrackingServiceTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private ReferralLinkRepository referralLinkRepository;

	@Mock
	private BlockedClickLogRepository blockedClickLogRepository;

	@Captor
	private ArgumentCaptor<BlockedClickLog> blockedClickLogCaptor;

	private ClickTrackingService clickTrackingService;

	@BeforeEach
	void setUp() {
		AffiliateClickTrackingProperties properties = new AffiliateClickTrackingProperties();
		properties.setMaxClicksPerWindow(5);
		properties.setWindow(Duration.ofMinutes(1));
		properties.setBlockedIpTtl(Duration.ofMinutes(5));
		properties.setBlockReason("RATE_LIMIT_EXCEEDED");

		clickTrackingService = new ClickTrackingService(
				stringRedisTemplate,
				referralLinkRepository,
				blockedClickLogRepository,
				properties
		);
	}

	// =========================================================
	// trackClick()
	// =========================================================

	@Test
	@DisplayName("trackClick: valid ref code increments click and passes")
	void trackClick_ValidReferral_IncrementsClick() {
		// Given
		String ip = "203.0.113.10";
		String rateLimitKey = ClickTrackingRedisKeys.rateLimitKey(ip);
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment(rateLimitKey)).willReturn(1L);
		given(referralLinkRepository.incrementTotalClicksByRefCode("REFA1B2C3")).willReturn(1);

		// When
		clickTrackingService.trackClick(" refa1b2c3 ", ip);

		// Then
		verify(valueOperations).increment(rateLimitKey);
		verify(stringRedisTemplate).expire(rateLimitKey, Duration.ofMinutes(1));
		verify(referralLinkRepository).incrementTotalClicksByRefCode("REFA1B2C3");
		verifyNoInteractions(blockedClickLogRepository);
		verify(valueOperations, never()).set(eq(ClickTrackingRedisKeys.blockedIpKey(ip)), anyString(), any(Duration.class));
	}

	@Test
	@DisplayName("trackClick: over limit blocks IP, logs audit and throws AFFILIATE_CLICK_RATE_LIMITED")
	void trackClick_ExceedRateLimit_BlocksIpAndThrows() {
		// Given
		String ip = "198.51.100.25";
		String rateLimitKey = ClickTrackingRedisKeys.rateLimitKey(ip);
		String blockedIpKey = ClickTrackingRedisKeys.blockedIpKey(ip);
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment(rateLimitKey)).willReturn(6L);

		// When + Then
		assertThatThrownBy(() -> clickTrackingService.trackClick("REFX9Y8Z7", ip))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AFFILIATE_CLICK_RATE_LIMITED);

		verify(valueOperations).set(blockedIpKey, "1", Duration.ofMinutes(5));
		verify(blockedClickLogRepository).save(blockedClickLogCaptor.capture());
		BlockedClickLog savedLog = blockedClickLogCaptor.getValue();
		assertThat(savedLog.getIpAddress()).isEqualTo(ip);
		assertThat(savedLog.getReason()).isEqualTo("RATE_LIMIT_EXCEEDED");
		assertThat(savedLog.getExpiresAt()).isNotNull();
		verify(stringRedisTemplate, never()).expire(rateLimitKey, Duration.ofMinutes(1));
		verify(referralLinkRepository, never()).incrementTotalClicksByRefCode(anyString());
	}

	@Test
	@DisplayName("trackClick: invalid ref code throws INVALID_INPUT")
	void trackClick_InvalidReferralCode_ThrowsInvalidInput() {
		// Given
		String ip = "192.0.2.44";
		String rateLimitKey = ClickTrackingRedisKeys.rateLimitKey(ip);
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment(rateLimitKey)).willReturn(2L);
		given(referralLinkRepository.incrementTotalClicksByRefCode("INVALIDCODE")).willReturn(0);

		// When + Then
		assertThatThrownBy(() -> clickTrackingService.trackClick("INVALIDCODE", ip))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);

		verify(stringRedisTemplate, never()).expire(rateLimitKey, Duration.ofMinutes(1));
		verify(blockedClickLogRepository, never()).save(any());
		verify(valueOperations, never()).set(eq(ClickTrackingRedisKeys.blockedIpKey(ip)), anyString(), any(Duration.class));
	}

	@Test
	@DisplayName("trackClick: blank ref code is rejected before touching Redis")
	void trackClick_BlankRefCode_ThrowsInvalidInput() {
		// When + Then
		assertThatThrownBy(() -> clickTrackingService.trackClick("   ", "127.0.0.1"))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);

		verifyNoInteractions(stringRedisTemplate, referralLinkRepository, blockedClickLogRepository);
	}

	@Test
	@DisplayName("getBlockedIps: returns active blocked IPs from repository query")
	void getBlockedIps_ReturnsActiveBlockedIpsFromRepository() {
		// Given
		LocalDateTime now = LocalDateTime.now();
		BlockedClickLog latestIpOne = createBlockedClickLog("198.51.100.10", "RATE_LIMIT_EXCEEDED", now.minusMinutes(1), now.plusMinutes(4));
		BlockedClickLog activeIpTwo = createBlockedClickLog("203.0.113.30", "RATE_LIMIT_EXCEEDED", now.minusMinutes(2), now.plusMinutes(3));

		given(blockedClickLogRepository.findActiveBlockedIps()).willReturn(List.of(
				latestIpOne,
				activeIpTwo
		));

		// When
		List<BlockedIpResponse> result = clickTrackingService.getBlockedIps();

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).ipAddress()).isEqualTo("198.51.100.10");
		assertThat(result.get(1).ipAddress()).isEqualTo("203.0.113.30");
	}

	@Test
	@DisplayName("unblockIp: removes blocked IP from DB and clears Redis keys")
	void unblockIp_RemovesBlockedIpFromDbAndRedis() {
		// Given
		String ip = "198.51.100.25";
		given(blockedClickLogRepository.deleteByIpAddress(ip)).willReturn(2L);

		// When
		clickTrackingService.unblockIp(ip);

		// Then
		verify(blockedClickLogRepository).deleteByIpAddress(ip);
		verify(stringRedisTemplate).delete(ClickTrackingRedisKeys.blockedIpKey(ip));
		verify(stringRedisTemplate).delete(ClickTrackingRedisKeys.rateLimitKey(ip));
	}

	@Test
	@DisplayName("unblockIp: missing blocked IP throws RESOURCE_NOT_FOUND")
	void unblockIp_MissingBlockedIp_ThrowsResourceNotFound() {
		// Given
		given(blockedClickLogRepository.deleteByIpAddress("198.51.100.99")).willReturn(0L);

		// When + Then
		assertThatThrownBy(() -> clickTrackingService.unblockIp("198.51.100.99"))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
	}

	private BlockedClickLog createBlockedClickLog(
			String ipAddress,
			String reason,
			LocalDateTime createdAt,
			LocalDateTime expiresAt
	) {
		BlockedClickLog log = new BlockedClickLog();
		log.setIpAddress(ipAddress);
		log.setReason(reason);
		log.setCreatedAt(createdAt);
		log.setExpiresAt(expiresAt);
		return log;
	}
}
