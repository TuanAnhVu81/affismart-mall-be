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
import com.affismart.mall.modules.product.entity.Category;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.product.repository.ProductRepository;
import com.affismart.mall.modules.user.entity.Role;
import com.affismart.mall.modules.user.entity.User;
import com.affismart.mall.modules.user.entity.UserRole;
import com.affismart.mall.modules.user.repository.RoleRepository;
import com.affismart.mall.modules.user.repository.UserRepository;
import com.affismart.mall.modules.user.repository.UserRoleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("AffiliateService Unit Tests")
class AffiliateServiceTest {

	@Mock
	private AffiliateAccountRepository affiliateAccountRepository;

	@Mock
	private ReferralLinkRepository referralLinkRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private UserRoleRepository userRoleRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private AffiliateMapper affiliateMapper;

	@InjectMocks
	private AffiliateService affiliateService;

	@Captor
	private ArgumentCaptor<AffiliateAccount> accountCaptor;

	@Captor
	private ArgumentCaptor<ReferralLink> referralLinkCaptor;

	// =========================================================
	// register()
	// =========================================================

	@Test
	@DisplayName("register: creates pending affiliate account and updates bank info")
	void register_ValidRequest_CreatesPendingAccount() {
		// Given
		Long userId = 11L;
		User user = createUser(userId, "customer@gmail.com");
		AffiliateRegisterRequest request = new AffiliateRegisterRequest(" TikTok ", " MB Bank - 123456789 ");
		AffiliateAccount savedAccount = createAffiliateAccount(100L, user, AffiliateAccountStatus.PENDING, "AFFX1Y2Z3");
		AffiliateAccountResponse expectedResponse = new AffiliateAccountResponse(
				100L,
				userId,
				savedAccount.getRefCode(),
				"TikTok",
				"PENDING",
				new BigDecimal("10.00"),
				BigDecimal.ZERO,
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		given(affiliateAccountRepository.existsByUser_Id(userId)).willReturn(false);
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(affiliateAccountRepository.existsByRefCode(anyString())).willReturn(false);
		given(affiliateAccountRepository.save(any(AffiliateAccount.class))).willReturn(savedAccount);
		given(affiliateMapper.toAffiliateAccountResponse(savedAccount)).willReturn(expectedResponse);

		// When
		AffiliateAccountResponse actual = affiliateService.register(userId, request);

		// Then
		verify(userRepository).save(user);
		verify(affiliateAccountRepository).save(accountCaptor.capture());
		AffiliateAccount persisted = accountCaptor.getValue();
		assertThat(persisted.getStatus()).isEqualTo(AffiliateAccountStatus.PENDING);
		assertThat(persisted.getPromotionChannel()).isEqualTo("TikTok");
		assertThat(persisted.getRefCode()).startsWith("AFF");
		assertThat(user.getBankInfo()).isEqualTo("MB Bank - 123456789");
		assertThat(actual).isEqualTo(expectedResponse);
	}

	@Test
	@DisplayName("register: existing affiliate account throws AFFILIATE_ACCOUNT_ALREADY_EXISTS")
	void register_AlreadyRegistered_ThrowsConflict() {
		// Given
		given(affiliateAccountRepository.existsByUser_Id(11L)).willReturn(true);

		// When + Then
		assertThatThrownBy(() -> affiliateService.register(
				11L,
				new AffiliateRegisterRequest("Facebook", "VCB-99887766")
		))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AFFILIATE_ACCOUNT_ALREADY_EXISTS);

		verifyNoInteractions(userRepository, roleRepository, userRoleRepository, productRepository);
		verify(affiliateAccountRepository, never()).save(any());
	}

	// =========================================================
	// updateAccountStatus()
	// =========================================================

	@Test
	@DisplayName("updateAccountStatus: APPROVED grants affiliate role when user does not have it")
	void updateAccountStatus_Approve_GrantsAffiliateRole() {
		// Given
		Long accountId = 20L;
		Long userId = 30L;
		User accountUser = createUser(userId, "customer@gmail.com");
		AffiliateAccount existingAccount = createAffiliateAccount(accountId, accountUser, AffiliateAccountStatus.PENDING, "AFF12345");
		AffiliateAccount savedAccount = createAffiliateAccount(accountId, accountUser, AffiliateAccountStatus.APPROVED, "AFF12345");

		User userWithRoles = createUser(userId, "customer@gmail.com");
		Role affiliateRole = createRole(3L, RoleName.AFFILIATE);
		AffiliateAccountResponse expectedResponse = new AffiliateAccountResponse(
				accountId,
				userId,
				"AFF12345",
				existingAccount.getPromotionChannel(),
				"APPROVED",
				new BigDecimal("10.00"),
				BigDecimal.ZERO,
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		given(affiliateAccountRepository.findWithUserById(accountId)).willReturn(Optional.of(existingAccount));
		given(affiliateAccountRepository.save(any(AffiliateAccount.class))).willReturn(savedAccount);
		given(userRepository.findWithRolesById(userId)).willReturn(Optional.of(userWithRoles));
		given(roleRepository.findByName(RoleName.AFFILIATE)).willReturn(Optional.of(affiliateRole));
		given(userRoleRepository.save(any(UserRole.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(affiliateMapper.toAffiliateAccountResponse(savedAccount)).willReturn(expectedResponse);

		// When
		AffiliateAccountResponse actual = affiliateService.updateAccountStatus(
				accountId,
				new UpdateAffiliateAccountStatusRequest(AffiliateAccountStatus.APPROVED)
		);

		// Then
		verify(affiliateAccountRepository).save(accountCaptor.capture());
		assertThat(accountCaptor.getValue().getStatus()).isEqualTo(AffiliateAccountStatus.APPROVED);
		verify(userRoleRepository).save(any(UserRole.class));
		assertThat(actual).isEqualTo(expectedResponse);
	}

	@Test
	@DisplayName("updateAccountStatus: setting PENDING is rejected with INVALID_INPUT")
	void updateAccountStatus_SetPending_ThrowsInvalidInput() {
		// Given
		Long accountId = 20L;
		AffiliateAccount existingAccount = createAffiliateAccount(
				accountId,
				createUser(30L, "customer@gmail.com"),
				AffiliateAccountStatus.APPROVED,
				"AFF12345"
		);
		given(affiliateAccountRepository.findWithUserById(accountId)).willReturn(Optional.of(existingAccount));

		// When + Then
		assertThatThrownBy(() -> affiliateService.updateAccountStatus(
				accountId,
				new UpdateAffiliateAccountStatusRequest(AffiliateAccountStatus.PENDING)
		))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);

		verify(affiliateAccountRepository, never()).save(any());
		verifyNoInteractions(userRoleRepository, roleRepository);
	}

	// =========================================================
	// createReferralLink()
	// =========================================================

	@Test
	@DisplayName("createReferralLink: non-approved account throws AFFILIATE_ACCOUNT_NOT_APPROVED")
	void createReferralLink_NonApprovedAccount_ThrowsForbidden() {
		// Given
		AffiliateAccount pendingAccount = createAffiliateAccount(
				100L,
				createUser(11L, "affiliate@gmail.com"),
				AffiliateAccountStatus.PENDING,
				"AFF12345"
		);
		given(affiliateAccountRepository.findByUser_Id(11L)).willReturn(Optional.of(pendingAccount));

		// When + Then
		assertThatThrownBy(() -> affiliateService.createReferralLink(11L, new CreateReferralLinkRequest(null)))
				.isInstanceOf(AppException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AFFILIATE_ACCOUNT_NOT_APPROVED);

		verify(referralLinkRepository, never()).save(any());
		verify(productRepository, never()).findByIdAndActiveTrue(anyLong());
	}

	@Test
	@DisplayName("createReferralLink: approved account creates link with generated ref code")
	void createReferralLink_ApprovedAccount_CreatesLink() {
		// Given
		Long userId = 50L;
		AffiliateAccount approvedAccount = createAffiliateAccount(
				200L,
				createUser(userId, "affiliate@gmail.com"),
				AffiliateAccountStatus.APPROVED,
				"AFF9988"
		);
		Product product = createProduct(9L, "SKU-9");
		ReferralLink savedLink = createReferralLink(321L, approvedAccount, product, "REFA1B2C3D4");
		ReferralLinkResponse expectedResponse = new ReferralLinkResponse(
				321L,
				200L,
				9L,
				"REFA1B2C3D4",
				0,
				0,
				true,
				LocalDateTime.now(),
				LocalDateTime.now()
		);

		given(affiliateAccountRepository.findByUser_Id(userId)).willReturn(Optional.of(approvedAccount));
		given(productRepository.findByIdAndActiveTrue(9L)).willReturn(Optional.of(product));
		given(referralLinkRepository.existsByRefCode(anyString())).willReturn(false);
		given(referralLinkRepository.save(any(ReferralLink.class))).willReturn(savedLink);
		given(affiliateMapper.toReferralLinkResponse(savedLink)).willReturn(expectedResponse);

		// When
		ReferralLinkResponse actual = affiliateService.createReferralLink(userId, new CreateReferralLinkRequest(9L));

		// Then
		verify(referralLinkRepository).save(referralLinkCaptor.capture());
		ReferralLink persistedLink = referralLinkCaptor.getValue();
		assertThat(persistedLink.getAffiliateAccount()).isEqualTo(approvedAccount);
		assertThat(persistedLink.getProduct()).isEqualTo(product);
		assertThat(persistedLink.getRefCode()).startsWith("REF");
		assertThat(actual).isEqualTo(expectedResponse);
	}

	// =========================================================
	// Private Helper Methods
	// =========================================================

	private User createUser(Long id, String email) {
		User user = new User();
		user.setId(id);
		user.setEmail(email);
		return user;
	}

	private Role createRole(Long id, RoleName name) {
		Role role = new Role();
		role.setId(id);
		role.setName(name);
		return role;
	}

	private AffiliateAccount createAffiliateAccount(Long id, User user, AffiliateAccountStatus status, String refCode) {
		AffiliateAccount account = new AffiliateAccount();
		account.setId(id);
		account.setUser(user);
		account.setStatus(status);
		account.setRefCode(refCode);
		account.setPromotionChannel("TikTok");
		account.setCommissionRate(new BigDecimal("10.00"));
		account.setBalance(BigDecimal.ZERO);
		return account;
	}

	private Product createProduct(Long id, String sku) {
		Product product = new Product();
		product.setId(id);
		product.setSku(sku);
		product.setName("Product " + id);
		product.setCategory(new Category());
		product.setActive(true);
		product.setPrice(new BigDecimal("100.00"));
		product.setStockQuantity(10);
		return product;
	}

	private ReferralLink createReferralLink(Long id, AffiliateAccount account, Product product, String refCode) {
		ReferralLink referralLink = new ReferralLink();
		referralLink.setId(id);
		referralLink.setAffiliateAccount(account);
		referralLink.setProduct(product);
		referralLink.setRefCode(refCode);
		referralLink.setTotalClicks(0);
		referralLink.setTotalConversions(0);
		referralLink.setActive(true);
		return referralLink;
	}
}
