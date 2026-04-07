package com.affismart.mall.common.enums;

public enum OrderStatus {
	PENDING,
	PAID,
	CONFIRMED,
	SHIPPED,
	DONE,
	CANCELLED;

	public boolean canTransitionTo(OrderStatus nextStatus) {
		if (nextStatus == null) {
			return false;
		}

		return switch (this) {
			case PENDING -> nextStatus == PAID || nextStatus == CANCELLED;
			case PAID -> nextStatus == CONFIRMED || nextStatus == CANCELLED;
			case CONFIRMED -> nextStatus == SHIPPED;
			case SHIPPED -> nextStatus == DONE;
			case DONE, CANCELLED -> false;
		};
	}
}
