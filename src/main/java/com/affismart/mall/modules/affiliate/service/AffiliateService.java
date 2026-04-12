package com.affismart.mall.modules.affiliate.service;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.common.enums.RoleName;
import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.affismart.mall.modules.affiliate.dto.request.AffiliateRegisterRequest;
import com.affismart.mall.modules.affiliate.dto.request.CreateReferralLinkRequest;
import com.affismart.mall.modules.affiliate.dto.request.UpdateAffiliateAccountStatusRequest;
import com.affismart.mall.modules.affiliate.dto.response.AffiliateAccountResponse;
import com.affismart.mall.modules.affiliate.dto.response.ReferralLinkResponse;
import com.affismart.mall.modules.affiliate.entity.AffiliateAccount;
import com.affismart.mall.modules.affiliate.entity.ReferralLink;
import com.affismart.mall.modules.affiliate.mapper.AffiliateMapper;
import com.affismart.mall.modules.affiliate.repository.AffiliateAccountRepository;
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
import java.util.Locale;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AffiliateService {

	private static final int MAX_CODE_RETRY = 30;
	private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final String ACCOUNT_REF_PREFIX = "AFF";
	private static final String LINK_REF_PREFIX = "REF";
	private static final int ACCOUNT_RANDOM_CODE_LENGTH = 8;
	private static final int LINK_RANDOM_CODE_LENGTH = 10;

	private final SecureRandom secureRandom = new SecureRandom();

	private final AffiliateAccountRepository affiliateAccountRepository;
	private final ReferralLinkRepository referralLinkRepository;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final ProductRepository productRepository;
	private final AffiliateMapper affiliateMapper;

	public AffiliateService(
			AffiliateAccountRepository affiliateAccountRepository,
			ReferralLinkRepository referralLinkRepository,
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			ProductRepository productRepository,
			AffiliateMapper affiliateMapper
	) {
		this.affiliateAccountRepository = affiliateAccountRepository;
		this.referralLinkRepository = referralLinkRepository;
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
}
