package com.affismart.mall.modules.payment.controller;

import com.affismart.mall.common.response.ApiResponse;
import com.affismart.mall.modules.auth.security.UserPrincipal;
import com.affismart.mall.modules.payment.dto.request.CreatePaymentSessionRequest;
import com.affismart.mall.modules.payment.dto.response.PaymentSessionResponse;
import com.affismart.mall.modules.payment.service.PaymentService;
import com.affismart.mall.modules.payment.service.PaymentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment", description = "Endpoints for checkout payment flow")
@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

	private final PaymentService paymentService;
	private final PaymentWebhookService paymentWebhookService;

	public PaymentController(PaymentService paymentService, PaymentWebhookService paymentWebhookService) {
		this.paymentService = paymentService;
		this.paymentWebhookService = paymentWebhookService;
	}

	@Operation(summary = "Create Stripe checkout session for a pending order (Customer only)")
	@PreAuthorize("hasRole('CUSTOMER')")
	@PostMapping("/create-session")
	public ApiResponse<PaymentSessionResponse> createPaymentSession(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreatePaymentSessionRequest request
	) {
		return ApiResponse.success(
				"Payment session created successfully",
				paymentService.createCheckoutSession(principal.getUserId(), request.orderId())
		);
	}

	@Operation(summary = "Verify Stripe webhook signature and accept event (Public)")
	@SecurityRequirements
	@PostMapping("/webhook")
	public ResponseEntity<String> handleStripeWebhook(
			@RequestBody String payload,
			@RequestHeader(name = "Stripe-Signature", required = false) String stripeSignature
	) {
		paymentWebhookService.handleVerifiedEvent(paymentWebhookService.verifyWebhook(payload, stripeSignature));
		return ResponseEntity.ok("ok");
	}
}
