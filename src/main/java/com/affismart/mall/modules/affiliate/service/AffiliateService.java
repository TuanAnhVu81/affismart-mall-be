package com.affismart.mall.modules.affiliate.service;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.common.enums.CommissionStatus;
import com.affismart.mall.common.enums.RoleName;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.common.response.PageResponse;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.affiliate.dto.request.AffiliateRegisterRequest;
import com.affismart.mall.modules.affiliate.dto.request.CreateReferralLinkRequest;
import com.affismart.mall.modules.affiliate.dto.request.UpdateAffiliateCommissionRateRequest;
import com.affismart.mall.modules.affiliate.dto.request.UpdateAffiliateAccountStatusRequest;
import com.affismart.mall.modules.affiliate.dto.request.UpdateReferralLinkStatusRequest;
import com.affismart.mall.modules.affiliate.dto.response.AdminAffiliateAccountResponse;
import com.affismart.mall.modules.affiliate.dto.response.AffiliateAccountResponse;
import com.affismart.mall.modules.affiliate.dto.response.AffiliateDashboardResponse;
import com.affismart.mall.modules.affiliate.dto.response.CommissionResponse;
import com.affismart.mall.modules.affiliate.dto.response.ReferralLinkResponse;
import com.affismart.mall.modules.affiliate.entity.AffiliateAccount;
import com.affismart.mall.modules.affiliate.entity.ReferralLink;
import com.affismart.mall.modules.affiliate.mapper.AffiliateMapper;
import com.affismart.mall.modules.affiliate.repository.AffiliateAccountRepository;
import com.affismart.mall.modules.affiliate.repository.CommissionRepository;
import com.affismart.mall.modules.affiliate.repository.CommissionSpecifications;
import com.affismart.mall.modules.affiliate.repository.ReferralLinkRepository;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.product.repository.ProductRepository;
import com.affismart.mall.modules.user.entity.Role;
import com.affismart.mall.modules.user.entity.User;
import com.affismart.mall.modules.user.entity.UserRole;
import com.affismart.mall.modules.user.repository.RoleRepository;
import com.affismart.mall.modules.user.repository.UserRepository;
import com.affismart.mall.modules.user.repository.UserRoleRepository;
import java.security.SecureRandom;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AffiliateService {

	private static final int MAX_CODE_RETRY = 30;
	private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final String ACCOUNT_REF_PREFIX = "AFF";
	private static final String LINK_REF_PREFIX = "REF";
	private static final int ACCOUNT_RANDOM_CODE_LENGTH = 8;
	private static final int LINK_RANDOM_CODE_LENGTH = 10;
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final Set<CommissionStatus> EARNED_COMMISSION_STATUSES = Set.of(
			CommissionStatus.APPROVED,
			CommissionStatus.PAID
	);

	private final SecureRandom secureRandom = new SecureRandom();

	private final AffiliateAccountRepository affiliateAccountRepository;
	private final ReferralLinkRepository referralLinkRepository;
	private final CommissionRepository commissionRepository;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final ProductRepository productRepository;
	private final AffiliateMapper affiliateMapper;

	public AffiliateService(
			AffiliateAccountRepository affiliateAccountRepository,
			ReferralLinkRepository referralLinkRepository,
			CommissionRepository commissionRepository,
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			ProductRepository productRepository,
			AffiliateMapper affiliateMapper
	) {
		this.affiliateAccountRepository = affiliateAccountRepository;
		this.referralLinkRepository = referralLinkRepository;
		this.commissionRepository = commissionRepository;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.productRepository = productRepository;
		this.affiliateMapper = affiliateMapper;
	}

	@Transactional
	public AffiliateAccountResponse register(Long userId, AffiliateRegisterRequest request) {
		if (affiliateAccountRepository.existsByUser_Id(userId)) {
			throw new AppException(ErrorCode.AFFILIATE_ACCOUNT_ALREADY_EXISTS);
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
		user.setBankInfo(request.bankInfo().trim());

		AffiliateAccount account = new AffiliateAccount();
		account.setUser(user);
		account.setPromotionChannel(request.promotionChannel().trim());
		account.setStatus(AffiliateAccountStatus.PENDING);
		account.setRefCode(generateUniqueCode(
				ACCOUNT_REF_PREFIX,
				ACCOUNT_RANDOM_CODE_LENGTH,
				affiliateAccountRepository::existsByRefCode
		));

		userRepository.save(user);
		AffiliateAccount savedAccount = affiliateAccountRepository.save(account);
		return affiliateMapper.toAffiliateAccountResponse(savedAccount);
	}

	@Transactional
	public AffiliateAccountResponse updateAccountStatus(Long accountId, UpdateAffiliateAccountStatusRequest request) {
		AffiliateAccount account = affiliateAccountRepository.findWithUserById(accountId)
				.orElseThrow(() -> new AppException(ErrorCode.AFFILIATE_ACCOUNT_NOT_FOUND));

		AffiliateAccountStatus targetStatus = request.status();
		if (targetStatus == AffiliateAccountStatus.PENDING) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Cannot set affiliate account status back to PENDING");
		}

		account.setStatus(targetStatus);
		AffiliateAccount savedAccount = affiliateAccountRepository.save(account);

		if (targetStatus == AffiliateAccountStatus.APPROVED) {
			grantAffiliateRole(savedAccount.getUser().getId());
		}

		return affiliateMapper.toAffiliateAccountResponse(savedAccount);
	}

	@Transactional
	public ReferralLinkResponse createReferralLink(Long userId, CreateReferralLinkRequest request) {
		AffiliateAccount account = affiliateAccountRepository.findByUser_Id(userId)
				.orElseThrow(() -> new AppException(ErrorCode.AFFILIATE_ACCOUNT_NOT_FOUND));

		if (account.getStatus() != AffiliateAccountStatus.APPROVED) {
			throw new AppException(ErrorCode.AFFILIATE_ACCOUNT_NOT_APPROVED);
		}

		Product product = resolveProduct(request.productId());

		ReferralLink referralLink = new ReferralLink();
		referralLink.setAffiliateAccount(account);
		referralLink.setProduct(product);
		referralLink.setRefCode(generateUniqueCode(
				LINK_REF_PREFIX,
				LINK_RANDOM_CODE_LENGTH,
				referralLinkRepository::existsByRefCode
		));

		ReferralLink savedLink = referralLinkRepository.save(referralLink);
		return affiliateMapper.toReferralLinkResponse(savedLink);
	}

	@Transactional(readOnly = true)
	public AffiliateAccountResponse getMyAffiliateAccount(Long userId) {
		return affiliateMapper.toAffiliateAccountResponse(getApprovedAffiliateAccountByUser(userId));
	}

	@Transactional(readOnly = true)
	public AffiliateDashboardResponse getMyDashboard(Long userId) {
		AffiliateAccount account = getApprovedAffiliateAccountByUser(userId);
		Long totalClicks = normalizeLong(referralLinkRepository.sumTotalClicksByAffiliateAccountId(account.getId()));
		long totalConversions = commissionRepository.countByAffiliateAccountIdAndStatusNot(
				account.getId(),
				CommissionStatus.REJECTED
		);
		BigDecimal totalCommissionEarned = defaultIfNull(
				commissionRepository.sumAmountByAffiliateAccountIdAndStatusIn(
						account.getId(),
						EARNED_COMMISSION_STATUSES
				)
		);
		BigDecimal conversionRate = calculateConversionRate(totalClicks, totalConversions);

		return new AffiliateDashboardResponse(
				totalClicks,
				totalConversions,
				conversionRate,
				defaultIfNull(account.getBalance()),
				totalCommissionEarned
		);
	}

	@Transactional(readOnly = true)
	public PageResponse<ReferralLinkResponse> getMyReferralLinks(
			Long userId,
			int page,
			int size,
			String sortBy,
			String sortDir,
			Boolean active
	) {
		AffiliateAccount account = getApprovedAffiliateAccountByUser(userId);
		Pageable pageable = PageRequest.of(
				Math.max(page, 0),
				normalizePageSize(size),
				Sort.by(resolveDirection(sortDir), normalizeReferralLinkSortProperty(sortBy))
		);

		Page<ReferralLinkResponse> responsePage = referralLinkRepository.findByAffiliateAccountId(
						account.getId(),
						active,
						pageable
				)
				.map(affiliateMapper::toReferralLinkResponse);
		return PageResponse.from(responsePage);
	}

	@Transactional
	public ReferralLinkResponse updateMyReferralLinkStatus(
			Long userId,
			Long referralLinkId,
			UpdateReferralLinkStatusRequest request
	) {
		AffiliateAccount account = getApprovedAffiliateAccountByUser(userId);
		ReferralLink referralLink = referralLinkRepository.findByIdAndAffiliateAccount_Id(referralLinkId, account.getId())
				.orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Referral link was not found"));

		referralLink.setActive(Boolean.TRUE.equals(request.active()));
		ReferralLink savedLink = referralLinkRepository.save(referralLink);
		return affiliateMapper.toReferralLinkResponse(savedLink);
	}

	@Transactional(readOnly = true)
	public PageResponse<CommissionResponse> getMyCommissions(
			Long userId,
			int page,
			int size,
			String sortBy,
			String sortDir,
			CommissionStatus status,
			LocalDateTime createdFrom,
			LocalDateTime createdTo
	) {
		validateDateRange(createdFrom, createdTo);
		AffiliateAccount account = getApprovedAffiliateAccountByUser(userId);
		Pageable pageable = PageRequest.of(
				Math.max(page, 0),
				normalizePageSize(size),
				Sort.by(resolveDirection(sortDir), normalizeCommissionSortProperty(sortBy))
		);

		Page<CommissionResponse> responsePage = commissionRepository.findAll(
						CommissionSpecifications.forAffiliateManagement(
								account.getId(),
								status,
								createdFrom,
								createdTo
						),
						pageable
				)
				.map(affiliateMapper::toCommissionResponse);
		return PageResponse.from(responsePage);
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminAffiliateAccountResponse> getAffiliateAccountsForAdmin(
			int page,
			int size,
			String sortBy,
			String sortDir,
			AffiliateAccountStatus status
	) {
		Pageable pageable = PageRequest.of(
				Math.max(page, 0),
				normalizePageSize(size),
				Sort.by(resolveDirection(sortDir), normalizeAffiliateAccountSortProperty(sortBy))
		);

		Page<AdminAffiliateAccountResponse> responsePage = (status == null
				? affiliateAccountRepository.findAll(pageable)
				: affiliateAccountRepository.findAllByStatus(status, pageable))
				.map(affiliateMapper::toAdminAffiliateAccountResponse);
		return PageResponse.from(responsePage);
	}

	@Transactional
	public AdminAffiliateAccountResponse updateCommissionRate(
			Long accountId,
			UpdateAffiliateCommissionRateRequest request
	) {
		AffiliateAccount account = affiliateAccountRepository.findWithUserById(accountId)
				.orElseThrow(() -> new AppException(ErrorCode.AFFILIATE_ACCOUNT_NOT_FOUND));

		account.setCommissionRate(request.commissionRate().setScale(2, RoundingMode.HALF_UP));
		AffiliateAccount savedAccount = affiliateAccountRepository.save(account);
		return affiliateMapper.toAdminAffiliateAccountResponse(savedAccount);
	}

	private void grantAffiliateRole(Long userId) {
		User user = userRepository.findWithRolesById(userId)
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

		boolean hasAffiliateRole = user.getUserRoles().stream()
				.anyMatch(userRole -> userRole.getRole().getName() == RoleName.AFFILIATE);
		if (hasAffiliateRole) {
			return;
		}

		Role affiliateRole = roleRepository.findByName(RoleName.AFFILIATE)
				.orElseThrow(() -> new AppException(ErrorCode.AFFILIATE_ROLE_NOT_FOUND));
		UserRole savedUserRole = userRoleRepository.save(new UserRole(user, affiliateRole));
		user.getUserRoles().add(savedUserRole);
	}

	private Product resolveProduct(Long productId) {
		if (productId == null) {
			return null;
		}
		return productRepository.findByIdAndActiveTrue(productId)
				.orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
	}

	private AffiliateAccount getApprovedAffiliateAccountByUser(Long userId) {
		AffiliateAccount account = affiliateAccountRepository.findWithUserByUser_Id(userId)
				.orElseThrow(() -> new AppException(ErrorCode.AFFILIATE_ACCOUNT_NOT_FOUND));
		if (account.getStatus() != AffiliateAccountStatus.APPROVED) {
			throw new AppException(ErrorCode.AFFILIATE_ACCOUNT_NOT_APPROVED);
		}
		return account;
	}

	private String generateUniqueCode(String prefix, int randomLength, Predicate<String> existsPredicate) {
		for (int attempt = 0; attempt < MAX_CODE_RETRY; attempt++) {
			String candidate = prefix + randomAlphaNumeric(randomLength);
			if (!existsPredicate.test(candidate)) {
				return candidate;
			}
		}
		throw new AppException(
				ErrorCode.REFERRAL_CODE_GENERATION_FAILED,
				String.format(Locale.ROOT, "Could not generate unique code for prefix %s", prefix)
		);
	}

	private String randomAlphaNumeric(int length) {
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int index = secureRandom.nextInt(CODE_ALPHABET.length());
			builder.append(CODE_ALPHABET.charAt(index));
		}
		return builder.toString();
	}

	private BigDecimal calculateConversionRate(Long totalClicks, long totalConversions) {
		if (totalClicks == null || totalClicks <= 0L || totalConversions <= 0L) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(totalConversions)
				.multiply(ONE_HUNDRED)
				.divide(BigDecimal.valueOf(totalClicks), 2, RoundingMode.HALF_UP);
	}

	private long normalizeLong(Long value) {
		return value == null ? 0L : value;
	}

	private BigDecimal defaultIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private Sort.Direction resolveDirection(String sortDir) {
		return "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
	}

	private int normalizePageSize(int size) {
		if (size <= 0) {
			return 10;
		}
		return Math.min(size, 100);
	}

	private String normalizeReferralLinkSortProperty(String sortBy) {
		if (!StringUtils.hasText(sortBy)) {
			return "createdAt";
		}
		return switch (sortBy.trim().toLowerCase(Locale.ROOT)) {
			case "id" -> "id";
			case "totalclicks", "total_clicks" -> "totalClicks";
			case "totalconversions", "total_conversions" -> "totalConversions";
			case "updatedat", "updated_at" -> "updatedAt";
			default -> "createdAt";
		};
	}

	private String normalizeCommissionSortProperty(String sortBy) {
		if (!StringUtils.hasText(sortBy)) {
			return "createdAt";
		}
		return switch (sortBy.trim().toLowerCase(Locale.ROOT)) {
			case "id" -> "id";
			case "amount" -> "amount";
			case "ratesnapshot", "rate_snapshot" -> "rateSnapshot";
			case "status" -> "status";
			case "updatedat", "updated_at" -> "updatedAt";
			default -> "createdAt";
		};
	}

	private String normalizeAffiliateAccountSortProperty(String sortBy) {
		if (!StringUtils.hasText(sortBy)) {
			return "createdAt";
		}
		return switch (sortBy.trim().toLowerCase(Locale.ROOT)) {
			case "id" -> "id";
			case "status" -> "status";
			case "commissionrate", "commission_rate" -> "commissionRate";
			case "balance" -> "balance";
			case "updatedat", "updated_at" -> "updatedAt";
			default -> "createdAt";
		};
	}

	private void validateDateRange(LocalDateTime createdFrom, LocalDateTime createdTo) {
		if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
			throw new AppException(ErrorCode.INVALID_INPUT, "createdFrom must be before or equal to createdTo");
		}
	}
}
