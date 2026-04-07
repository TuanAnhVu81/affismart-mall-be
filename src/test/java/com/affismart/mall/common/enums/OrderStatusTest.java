package com.affismart.mall.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderStatus Unit Tests")
class OrderStatusTest {

	@Test
	@DisplayName("PENDING can only move to PAID or CANCELLED")
	void pending_AllowsOnlyPaidOrCancelled() {
		assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PAID)).isTrue();
		assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
		assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
		assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.DONE)).isFalse();
	}

	@Test
	@DisplayName("PAID can only move to CONFIRMED or CANCELLED")
	void paid_AllowsOnlyConfirmedOrCancelled() {
		assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
		assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
		assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
		assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.PENDING)).isFalse();
	}

	@Test
	@DisplayName("CONFIRMED can only move to SHIPPED")
	void confirmed_AllowsOnlyShipped() {
		assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
		assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DONE)).isFalse();
		assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
	}

	@Test
	@DisplayName("SHIPPED can only move to DONE")
	void shipped_AllowsOnlyDone() {
		assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DONE)).isTrue();
		assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
		assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PAID)).isFalse();
	}

	@Test
	@DisplayName("DONE and CANCELLED are terminal states")
	void doneAndCancelled_AreTerminalStates() {
		assertThat(OrderStatus.DONE.canTransitionTo(OrderStatus.PENDING)).isFalse();
		assertThat(OrderStatus.DONE.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
		assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PAID)).isFalse();
		assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DONE)).isFalse();
	}

	@Test
	@DisplayName("canTransitionTo returns false when nextStatus is null")
	void canTransitionTo_NullStatus_ReturnsFalse() {
		assertThat(OrderStatus.PENDING.canTransitionTo(null)).isFalse();
	}
}
