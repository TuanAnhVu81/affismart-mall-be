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
import java.util.Locale;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ClickTrackingService {

	private static final String BLOCKED_IP_MARKER = "1";

	private final StringRedisTemplate stringRedisTemplate;
	private final ReferralLinkRepository referralLinkRepository;
	private final BlockedClickLogRepository blockedClickLogRepository;
	private final int maxClicksPerWindow;
	private final Duration windowDuration;
	private final Duration blockedIpTtl;
	private final String blockReason;

	public ClickTrackingService(
			StringRedisTemplate stringRedisTemplate,
			ReferralLinkRepository referralLinkRepository,
			BlockedClickLogRepository blockedClickLogRepository,
			AffiliateClickTrackingProperties clickTrackingProperties
	) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.referralLinkRepository = referralLinkRepository;
		this.blockedClickLogRepository = blockedClickLogRepository;
		this.maxClicksPerWindow = Math.max(clickTrackingProperties.getMaxClicksPerWindow(), 1);
		this.windowDuration = defaultIfInvalid(clickTrackingProperties.getWindow(), Duration.ofMinutes(1));
		this.blockedIpTtl = defaultIfInvalid(clickTrackingProperties.getBlockedIpTtl(), Duration.ofMinutes(5));
		this.blockReason = resolveBlockReason(clickTrackingProperties.getBlockReason());
	}

	@Transactional
	public void trackClick(String refCode, String ipAddress) {
		String normalizedRefCode = normalizeRefCode(refCode);
		String normalizedIp = normalizeIp(ipAddress);

		long clickCount = increaseRateCounter(normalizedIp);
		if (clickCount > maxClicksPerWindow) {
			blockIp(normalizedIp);
			throw new AppException(ErrorCode.AFFILIATE_CLICK_RATE_LIMITED);
		}

		int updatedRows = referralLinkRepository.incrementTotalClicksByRefCode(normalizedRefCode);
		if (updatedRows == 0) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Referral code is invalid or inactive");
		}
	}

	@Transactional(readOnly = true)
	public List<BlockedIpResponse> getBlockedIps() {
		return blockedClickLogRepository.findActiveBlockedIps()
				.stream()
				.map(log -> new BlockedIpResponse(
						log.getIpAddress(),
						log.getReason(),
						log.getCreatedAt(),
						log.getExpiresAt()
				))
				.toList();
	}

	@Transactional
	public void unblockIp(String ipAddress) {
		String normalizedIp = normalizeIp(ipAddress);
		long deletedRows = blockedClickLogRepository.deleteByIpAddress(normalizedIp);
		if (deletedRows == 0) {
			throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Blocked IP was not found");
		}

		stringRedisTemplate.delete(ClickTrackingRedisKeys.blockedIpKey(normalizedIp));
		stringRedisTemplate.delete(ClickTrackingRedisKeys.rateLimitKey(normalizedIp));
	}

	private long increaseRateCounter(String ipAddress) {
		String rateLimitKey = ClickTrackingRedisKeys.rateLimitKey(ipAddress);
		Long currentCount = stringRedisTemplate.opsForValue().increment(rateLimitKey);
		if (currentCount == null) {
			throw new IllegalStateException("Failed to increase click counter in Redis");
		}
		if (currentCount == 1L) {
			stringRedisTemplate.expire(rateLimitKey, windowDuration);
		}
		return currentCount;
	}

	private void blockIp(String ipAddress) {
		LocalDateTime now = LocalDateTime.now();
		String blockedIpKey = ClickTrackingRedisKeys.blockedIpKey(ipAddress);
		if (blockedIpTtl.isZero() || blockedIpTtl.isNegative()) {
			stringRedisTemplate.opsForValue().set(blockedIpKey, BLOCKED_IP_MARKER);
		} else {
			stringRedisTemplate.opsForValue().set(blockedIpKey, BLOCKED_IP_MARKER, blockedIpTtl);
		}

		BlockedClickLog log = new BlockedClickLog();
		log.setIpAddress(ipAddress);
		log.setReason(blockReason);
		log.setExpiresAt(blockedIpTtl.isZero() || blockedIpTtl.isNegative() ? null : now.plus(blockedIpTtl));
		blockedClickLogRepository.save(log);
	}

	private String normalizeRefCode(String refCode) {
		if (!StringUtils.hasText(refCode)) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Referral code is required");
		}
		return refCode.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeIp(String ipAddress) {
		if (!StringUtils.hasText(ipAddress)) {
			return "unknown";
		}
		return ipAddress.trim();
	}

	private Duration defaultIfInvalid(Duration value, Duration fallback) {
		if (value == null || value.isZero() || value.isNegative()) {
			return fallback;
		}
		return value;
	}

	private String resolveBlockReason(String reason) {
		if (!StringUtils.hasText(reason)) {
			return "RATE_LIMIT_EXCEEDED";
		}
		return reason.trim();
	}
}
