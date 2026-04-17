package com.affismart.mall.modules.affiliate.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.common.util.ClientIpResolver;
import com.affismart.mall.modules.affiliate.dto.request.AffiliateRegisterRequest;
import com.affismart.mall.modules.affiliate.dto.request.CreateReferralLinkRequest;
import com.affismart.mall.modules.affiliate.dto.request.TrackClickRequest;
import com.affismart.mall.modules.affiliate.dto.request.UpdateAffiliateAccountStatusRequest;
import com.affismart.mall.modules.affiliate.dto.request.UpdatePayoutRequestStatusRequest;
import com.affismart.mall.modules.affiliate.dto.response.AffiliateAccountResponse;
import com.affismart.mall.modules.affiliate.dto.response.PayoutRequestResponse;
import com.affismart.mall.modules.affiliate.dto.response.ReferralLinkResponse;
import com.affismart.mall.modules.affiliate.service.AffiliateService;
import com.affismart.mall.modules.affiliate.service.ClickTrackingService;
import com.affismart.mall.modules.affiliate.service.PayoutService;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Affiliate", description = "Endpoints for affiliate application and referral link management")
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

	@Operation(summary = "Update affiliate account status (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/accounts/{id}/status")
	public ApiResponse<AffiliateAccountResponse> updateAffiliateAccountStatus(
			@PathVariable Long id,
			@Valid @RequestBody UpdateAffiliateAccountStatusRequest request
	) {
		return ApiResponse.success(
				"Affiliate account status updated successfully",
				affiliateService.updateAccountStatus(id, request)
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

	@Operation(summary = "Track referral click (Public)")
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

	@Operation(summary = "Update payout request status (Admin only)")
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/payouts/{id}/status")
	public ApiResponse<PayoutRequestResponse> updatePayoutStatus(
			@PathVariable Long id,
			@Valid @RequestBody UpdatePayoutRequestStatusRequest request
	) {
		return ApiResponse.success(
				"Payout request status updated successfully",
				payoutService.updatePayoutStatus(id, request)
		);
	}
}
