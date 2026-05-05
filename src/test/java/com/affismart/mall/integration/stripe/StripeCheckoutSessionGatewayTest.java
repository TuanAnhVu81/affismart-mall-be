package com.affismart.mall.integration.stripe;

import com.affismart.mall.modules.order.entity.Order;
import com.affismart.mall.modules.order.entity.OrderItem;
import com.affismart.mall.modules.payment.model.CheckoutSessionResult;
import com.affismart.mall.modules.payment.service.PaymentRedirectService;
import com.affismart.mall.modules.product.entity.Product;
import com.affismart.mall.modules.user.entity.User;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@DisplayName("StripeCheckoutSessionGateway Unit Tests")
class StripeCheckoutSessionGatewayTest {

	@Test
	@DisplayName("createCheckoutSession: sends stable idempotency key per order")
	void createCheckoutSession_ValidOrder_SendsOrderIdempotencyKey() throws Exception {
		// Given
		StripeProperties stripeProperties = createStripeProperties();
		PaymentRedirectService paymentRedirectService = mock(PaymentRedirectService.class);
		given(paymentRedirectService.getStripeSuccessReturnUrl()).willReturn("http://localhost:8080/api/v1/payment/success?session_id={CHECKOUT_SESSION_ID}");
		given(paymentRedirectService.getStripeCancelReturnUrl()).willReturn("http://localhost:8080/api/v1/payment/cancel");

		StripeCheckoutSessionGateway gateway = new StripeCheckoutSessionGateway(stripeProperties, paymentRedirectService);
		Order order = createOrder(77L, 12L);
		OrderItem item = createOrderItem(order);

		Session stripeSession = new Session();
		stripeSession.setId("cs_test_77");
		stripeSession.setUrl("https://checkout.stripe.com/c/pay/cs_test_77");

		ArgumentCaptor<RequestOptions> requestOptionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);
		try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
			mockedSession.when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
					.thenReturn(stripeSession);

			// When
			CheckoutSessionResult result = gateway.createCheckoutSession(order, List.of(item));

			// Then
			assertThat(result.sessionId()).isEqualTo("cs_test_77");
			assertThat(result.checkoutUrl()).isEqualTo("https://checkout.stripe.com/c/pay/cs_test_77");
			mockedSession.verify(() -> Session.create(any(SessionCreateParams.class), requestOptionsCaptor.capture()));
		}

		RequestOptions requestOptions = requestOptionsCaptor.getValue();
		assertThat(requestOptions.getApiKey()).isEqualTo("sk_test_dummy");
		assertThat(requestOptions.getIdempotencyKey()).isEqualTo("checkout-session:order:77");
	}

	private StripeProperties createStripeProperties() {
		StripeProperties properties = new StripeProperties();
		properties.setSecretKey("sk_test_dummy");
		properties.setRedirectBaseUrl("http://localhost:8080");
		properties.setSuccessUrl("http://localhost:3000/payment/success?session_id={CHECKOUT_SESSION_ID}");
		properties.setCancelUrl("http://localhost:3000/payment/cancel");
		properties.setCurrency("vnd");
		return properties;
	}

	private Order createOrder(Long orderId, Long userId) {
		User user = new User();
		user.setId(userId);
		user.setEmail("customer@example.com");
		user.setFullName("Customer");
		user.setPasswordHash("password");

		Order order = new Order();
		order.setId(orderId);
		order.setUser(user);
		order.setShippingAddress("Ho Chi Minh");
		return order;
	}

	private OrderItem createOrderItem(Order order) {
		Product product = new Product();
		product.setId(5L);
		product.setName("Cotton T-Shirt");

		OrderItem item = new OrderItem();
		item.setOrder(order);
		item.setProduct(product);
		item.setQuantity(2);
		item.setPriceAtTime(new BigDecimal("150000"));
		return item;
	}
}
