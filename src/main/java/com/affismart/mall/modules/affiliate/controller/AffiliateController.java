package com.affismart.mall.modules.affiliate.controller;

import com.affismart.mall.common.enums.AffiliateAccountStatus;
import com.affismart.mall.common.enums.CommissionStatus;
import com.affismart.mall.common.enums.PayoutRequestStatus;
import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.common.response.PageResponse;
import com.affismart.mall.common.util.ClientIpResolver;
import com.affismart.mall.modules.affiliate.dto.request.AffiliateRegisterRequest;
import com.affismart.mall.modules.affiliate.dto.request.CreateReferralLinkRequest;
import com.affismart.mall.modules.affiliate.dto.request.TrackClickRequest;
import com.affismart.mall.modules.affiliate.dto.request.UpdateAffiliateAccountStatusRequest;
import com.affismart.mall.modules.affiliate.dto.request.UpdateAffiliateCommissionRateRequest;
import com.affismart.mall.modules.affiliate.dto.request.UpdatePayoutRequestStatusRequest;
import com.affismart.mall.modules.affiliate.dto.request.UpdateReferralLinkStatusRequest;
import com.affismart.mall.modules.affiliate.dto.response.AdminAffiliateAccountResponse;
import com.affismart.mall.modules.affiliate.dto.response.AdminPayoutRequestResponse;
import com.affismart.mall.modules.affiliate.dto.response.AffiliateAccountResponse;
import com.affismart.mall.modules.affiliate.dto.response.AffiliateDashboardResponse;
import com.affismart.mall.modules.affiliate.dto.response.BlockedIpResponse;
import com.affismart.mall.modules.affiliate.dto.response.CommissionResponse;
import com.affismart.mall.modules.affiliate.dto.response.PayoutRequestResponse;
import com.affismart.mall.modules.affiliate.dto.response.ReferralLinkResponse;
import com.affismart.mall.modules.affiliate.service.AffiliateService;
import com.affismart.mall.modules.affiliate.service.ClickTrackingService;
import com.affismart.mall.modules.affiliate.service.PayoutService;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Affiliate", description = "Endpoints for affiliate application and referral link management")
@Validated
@RestController
@RequestMapping("/api/v1/affiliate")
public class AffiliateController {

	private final AffiliateService affiliateService;
	private final ClickTrackingService clickTrackingService;
	private final PayoutService payoutService;

	public AffiliateController(
			AffiliateService affiliateService,
			ClickTrackingService clickTrackingService,
			PayoutService payoutService
	) {
		this.affiliateService = affiliateService;
		this.clickTrackingService = clickTrackingService;
		this.payoutService = payoutService;
	}

	@Operation(summary = "Register as affiliate (Customer only)")
	@PreAuthorize("hasRole('CUSTOMER')")
	@PostMapping("/register")
	public ApiResponse<AffiliateAccountResponse> registerAffiliate(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody AffiliateRegisterRequest request
	) {
		return ApiResponse.success(
				"Affiliate registration submitted successfully",
				affiliateService.register(principal.getUserId(), request)
		);
	}

	@Operation(summary = "Get current affiliate account (Affiliate only)")
	@PreAuthorize("hasRole('AFFILIATE')")
	@GetMapping("/me")
	public ApiResponse<AffiliateAccountResponse> getMyAffiliateAccount(
			@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(
				"Affiliate account retrieved successfully",
				affiliateService.getMyAffiliateAccount(principal.getUserId())
		);
	}

	@Operation(summary = "Get affiliate dashboard summary (Affiliate only)")
	@PreAuthorize("hasRole('AFFILIATE')")
	@GetMapping("/me/dashboard")
	public ApiResponse<AffiliateDashboardResponse> getMyAffiliateDashboard(
			@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(
				"Affiliate dashboard retrieved successfully",
				affiliateService.getMyDashboard(principal.getUserId())
		);
	}

	@Operation(summary = "Create referral link (Affiliate only)")
	@PreAuthorize("hasRole('AFFILIATE')")
	@PostMapping("/me/links")
	public ApiResponse<ReferralLinkResponse> createReferralLink(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreateReferralLinkRequest request
	) {
		return ApiResponse.success(
				"Referral link created successfully",
				affiliateService.createReferralLink(principal.getUserId(), request)
		);
	}

	@Operation(summary = "Get my referral links (Affiliate only)")
	@PreAuthorize("hasRole('AFFILIATE')")
	@GetMapping("/me/links")
	public ApiResponse<PageResponse<ReferralLinkResponse>> getMyReferralLinks(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(name = "sort_by", defaultValue = "createdAt") String sortBy,
			@RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir,
			@RequestParam(required = false) Boolean active
	) {
		return ApiResponse.success(
				"Referral links retrieved successfully",
				affiliateService.getMyReferralLinks(principal.getUserId(), page, size, sortBy, sortDir, active)
		);
	}

	@Operation(summary = "Update my referral link status (Affiliate only)")
	@PreAuthorize("hasRole('AFFILIATE')")
	@PutMapping("/me/links/{id}/status")
	public ApiResponse<ReferralLinkResponse> updateMyReferralLinkStatus(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateReferralLinkStatusRequest request
	) {
		return ApiResponse.success(
				"Referral link status updated successfully",
				affiliateService.updateMyReferralLinkStatus(principal.getUserId(), id, request)
		);
	}

	@Operation(summary = "Get my commissions (Affiliate only)")
	@PreAuthorize("hasRole('AFFILIATE')")
	@GetMapping("/me/commissions")
	public ApiResponse<PageResponse<CommissionResponse>> getMyCommissions(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(name = "sort_by", defaultValue = "createdAt") String sortBy,
			@RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir,
			@RequestParam(required = false) CommissionStatus status,
			@RequestParam(name = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
			@RequestParam(name = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo
	) {
		return ApiResponse.success(
				"Commissions retrieved successfully",
				affiliateService.getMyCommissions(
						principal.getUserId(),
						page,
						size,
						sortBy,
						sortDir,
						status,
						createdFrom,
						createdTo
				)
		);
	}

	@Operation(summary = "Track referral click (Public)")
	@SecurityRequirements
	@PostMapping("/track-click")
	public ApiResponse<Void> trackClick(
			@Valid @RequestBody TrackClickRequest request,
			HttpServletRequest httpServletRequest
	) {
		String clientIp = ClientIpResolver.resolve(httpServletRequest);
		clickTrackingService.trackClick(request.refCode(), clientIp);
		return ApiResponse.success("Valid");
	}

	@Operation(summary = "Create payout request from approved commissions (Affiliate only)")
	@PreAuthorize("hasRole('AFFILIATE')")
	@PostMapping("/me/payouts")
	public ApiResponse<PayoutRequestResponse> createMyPayoutRequest(
			@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(
				"Payout request created successfully",
				payoutService.createMyPayoutRequest(principal.getUserId())
		);
	}

	@Operation(summary = "Get my payout requests (Affiliate only)")
	@PreAuthorize("hasRole('AFFILIATE')")
	@GetMapping("/me/payouts")
	public ApiResponse<PageResponse<PayoutRequestResponse>> getMyPayoutRequests(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(name = "sort_by", defaultValue = "createdAt") String sortBy,
			@RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir,
			@RequestParam(required = false) PayoutRequestStatus status
	) {
		return ApiResponse.success(
				"Payout requests retrieved successfully",
				payoutService.getMyPayoutRequests(principal.getUserId(), page, size, sortBy, sortDir, status)
		);
	}

	@Operation(summary = "Get all affiliate accounts (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/accounts")
	public ApiResponse<PageResponse<AdminAffiliateAccountResponse>> getAffiliateAccounts(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(name = "sort_by", defaultValue = "createdAt") String sortBy,
			@RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir,
			@RequestParam(required = false) AffiliateAccountStatus status
	) {
		return ApiResponse.success(
				"Affiliate accounts retrieved successfully",
				affiliateService.getAffiliateAccountsForAdmin(page, size, sortBy, sortDir, status)
		);
	}

	@Operation(summary = "Update affiliate account status (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/accounts/{id}/status")
	public ApiResponse<AffiliateAccountResponse> updateAffiliateAccountStatus(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateAffiliateAccountStatusRequest request
	) {
		return ApiResponse.success(
				"Affiliate account status updated successfully",
				affiliateService.updateAccountStatus(id, request)
		);
	}

	@Operation(summary = "Update affiliate commission rate (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/accounts/{id}/commission-rate")
	public ApiResponse<AdminAffiliateAccountResponse> updateAffiliateCommissionRate(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateAffiliateCommissionRateRequest request
	) {
		return ApiResponse.success(
				"Affiliate commission rate updated successfully",
				affiliateService.updateCommissionRate(id, request)
		);
	}

	@Operation(summary = "Get all payout requests (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/payouts")
	public ApiResponse<PageResponse<AdminPayoutRequestResponse>> getPayoutRequests(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
			@RequestParam(name = "sort_by", defaultValue = "createdAt") String sortBy,
			@RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir,
			@RequestParam(required = false) PayoutRequestStatus status
	) {
		return ApiResponse.success(
				"Payout requests retrieved successfully",
				payoutService.getPayoutRequestsForAdmin(page, size, sortBy, sortDir, status)
		);
	}

	@Operation(summary = "Update payout request status (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/payouts/{id}/status")
	public ApiResponse<PayoutRequestResponse> updatePayoutStatus(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdatePayoutRequestStatusRequest request
	) {
		return ApiResponse.success(
				"Payout request status updated successfully",
				payoutService.updatePayoutStatus(id, request)
		);
	}

	@Operation(summary = "Get blocked IP list for affiliate click tracking (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/blocked-ips")
	public ApiResponse<List<BlockedIpResponse>> getBlockedIps() {
		return ApiResponse.success(
				"Blocked IPs retrieved successfully",
				clickTrackingService.getBlockedIps()
		);
	}

	@Operation(summary = "Remove one blocked IP from affiliate click tracking (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/blocked-ips/{ip}")
	public ApiResponse<Void> unblockIp(@PathVariable @NotBlank @Size(max = 45) String ip) {
		clickTrackingService.unblockIp(ip);
		return ApiResponse.success("Blocked IP removed successfully");
	}
}
