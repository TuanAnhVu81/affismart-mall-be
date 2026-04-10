package com.affismart.mall.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	INVALID_INPUT("INVALID_INPUT", "Invalid request payload", HttpStatus.BAD_REQUEST),
	UNAUTHORIZED("UNAUTHORIZED", "Authentication is required", HttpStatus.UNAUTHORIZED),
	ACCESS_DENIED("ACCESS_DENIED", "You do not have permission to access this resource", HttpStatus.FORBIDDEN),
	RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Requested resource was not found", HttpStatus.NOT_FOUND),
	CONFLICT("CONFLICT", "Request could not be completed because of a conflict", HttpStatus.CONFLICT),
	USER_NOT_FOUND("USER_NOT_FOUND", "User was not found", HttpStatus.NOT_FOUND),
	EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "Email is already registered", HttpStatus.CONFLICT),
	INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED),
	INVALID_CURRENT_PASSWORD("INVALID_CURRENT_PASSWORD", "Current password is incorrect", HttpStatus.BAD_REQUEST),
	USER_NOT_ACTIVE("USER_NOT_ACTIVE", "User account is not active", HttpStatus.FORBIDDEN),
	INVALID_USER_STATUS("INVALID_USER_STATUS", "User status transition is not allowed", HttpStatus.BAD_REQUEST),
	DEFAULT_ROLE_NOT_FOUND(
			"DEFAULT_ROLE_NOT_FOUND",
			"Default customer role is not configured",
			HttpStatus.INTERNAL_SERVER_ERROR
	),
	ADMIN_ROLE_NOT_FOUND(
			"ADMIN_ROLE_NOT_FOUND",
			"Administrator role is not configured",
			HttpStatus.INTERNAL_SERVER_ERROR
	),
	INVALID_REFRESH_TOKEN("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired", HttpStatus.UNAUTHORIZED),
	REFRESH_TOKEN_REUSE_DETECTED(
			"REFRESH_TOKEN_REUSE_DETECTED",
			"Refresh token reuse detected. All sessions have been revoked",
			HttpStatus.UNAUTHORIZED
	),
	CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", "Category was not found", HttpStatus.NOT_FOUND),
	CATEGORY_SLUG_ALREADY_EXISTS("CATEGORY_SLUG_ALREADY_EXISTS", "Category slug already exists", HttpStatus.CONFLICT),
	PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "Product was not found", HttpStatus.NOT_FOUND),
	PRODUCT_SKU_ALREADY_EXISTS("PRODUCT_SKU_ALREADY_EXISTS", "Product SKU already exists", HttpStatus.CONFLICT),
	PRODUCT_SLUG_ALREADY_EXISTS("PRODUCT_SLUG_ALREADY_EXISTS", "Product slug already exists", HttpStatus.CONFLICT),
	ORDER_NOT_FOUND("ORDER_NOT_FOUND", "Order was not found", HttpStatus.NOT_FOUND),
	ORDER_CANCELLATION_NOT_ALLOWED(
			"ORDER_CANCELLATION_NOT_ALLOWED",
			"Order cancellation is not allowed for current status",
			HttpStatus.CONFLICT
	),
	ORDER_STATUS_TRANSITION_NOT_ALLOWED(
			"ORDER_STATUS_TRANSITION_NOT_ALLOWED",
			"Order status transition is not allowed",
			HttpStatus.CONFLICT
	),
	ORDER_PAYMENT_NOT_ALLOWED(
			"ORDER_PAYMENT_NOT_ALLOWED",
			"Order is not eligible for payment session creation",
			HttpStatus.CONFLICT
	),
	ORDER_PAYMENT_SESSION_ALREADY_EXISTS(
			"ORDER_PAYMENT_SESSION_ALREADY_EXISTS",
			"Payment session already exists for this order",
			HttpStatus.CONFLICT
	),
	PAYMENT_GATEWAY_NOT_CONFIGURED(
			"PAYMENT_GATEWAY_NOT_CONFIGURED",
			"Payment gateway is not configured",
			HttpStatus.SERVICE_UNAVAILABLE
	),
	PAYMENT_SESSION_CREATION_FAILED(
			"PAYMENT_SESSION_CREATION_FAILED",
			"Failed to create payment session",
			HttpStatus.BAD_GATEWAY
	),
	PAYMENT_WEBHOOK_SIGNATURE_INVALID(
			"PAYMENT_WEBHOOK_SIGNATURE_INVALID",
			"Stripe webhook signature is invalid",
			HttpStatus.BAD_REQUEST
	),
	PAYMENT_WEBHOOK_CONFIG_INVALID(
			"PAYMENT_WEBHOOK_CONFIG_INVALID",
			"Stripe webhook configuration is invalid",
			HttpStatus.SERVICE_UNAVAILABLE
	),
	PAYMENT_REFUND_FAILED("PAYMENT_REFUND_FAILED", "Failed to refund payment for order cancellation", HttpStatus.BAD_GATEWAY),
	FILE_STORAGE_NOT_CONFIGURED(
			"FILE_STORAGE_NOT_CONFIGURED",
			"File storage provider is not configured",
			HttpStatus.SERVICE_UNAVAILABLE
	),
	FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "Failed to upload file", HttpStatus.BAD_GATEWAY),
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;

	ErrorCode(String code, String message, HttpStatus httpStatus) {
		this.code = code;
		this.message = message;
		this.httpStatus = httpStatus;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
}
