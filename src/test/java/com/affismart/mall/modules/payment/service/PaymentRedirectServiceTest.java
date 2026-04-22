package com.affismart.mall.modules.payment.service;

import com.affismart.mall.integration.stripe.StripeProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRedirectService Unit Tests")
class PaymentRedirectServiceTest {

	@Mock
	private StripeProperties stripeProperties;

	@InjectMocks
	private PaymentRedirectService paymentRedirectService;

	@Test
	@DisplayName("getStripeSuccessReturnUrl: builds backend success endpoint with session placeholder")
	void getStripeSuccessReturnUrl_BuildsBackendUrl() {
		given(stripeProperties.getRedirectBaseUrl()).willReturn("https://api.affismart.com");

		String result = paymentRedirectService.getStripeSuccessReturnUrl();

		assertThat(result).isEqualTo("https://api.affismart.com/api/v1/payment/success?session_id={CHECKOUT_SESSION_ID}");
	}

	@Test
	@DisplayName("getStripeCancelReturnUrl: builds backend cancel endpoint")
	void getStripeCancelReturnUrl_BuildsBackendUrl() {
		given(stripeProperties.getRedirectBaseUrl()).willReturn("https://api.affismart.com");

		String result = paymentRedirectService.getStripeCancelReturnUrl();

		assertThat(result).isEqualTo("https://api.affismart.com/api/v1/payment/cancel");
	}

	@Test
	@DisplayName("redirectToFrontendSuccess: returns 302 redirect and preserves session id")
	void redirectToFrontendSuccess_ReturnsRedirectResponse() {
		given(stripeProperties.getSuccessUrl()).willReturn("https://shop.affismart.com/payment/success");

		ResponseEntity<Void> response = paymentRedirectService.redirectToFrontendSuccess("cs_test_123");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
				.isEqualTo("https://shop.affismart.com/payment/success?session_id=cs_test_123");
	}

	@Test
	@DisplayName("redirectToFrontendCancel: returns 302 redirect to frontend cancel page")
	void redirectToFrontendCancel_ReturnsRedirectResponse() {
		given(stripeProperties.getCancelUrl()).willReturn("https://shop.affismart.com/payment/cancel");

		ResponseEntity<Void> response = paymentRedirectService.redirectToFrontendCancel();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
				.isEqualTo("https://shop.affismart.com/payment/cancel");
	}
}
