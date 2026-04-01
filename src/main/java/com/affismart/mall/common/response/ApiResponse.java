package com.affismart.mall.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
		boolean success,
		String message,
		String errorCode,
		T data,
		Map<String, String> errors,
		OffsetDateTime timestamp
) {

	public static <T> ApiResponse<T> success(String message, T data) {
		return ApiResponse.<T>builder()
				.success(true)
				.message(message)
				.data(data)
				.timestamp(OffsetDateTime.now())
				.build();
	}

	public static ApiResponse<Void> success(String message) {
		return success(message, null);
	}

	public static ApiResponse<Void> error(String errorCode, String message) {
		return ApiResponse.<Void>builder()
				.success(false)
				.errorCode(errorCode)
				.message(message)
				.timestamp(OffsetDateTime.now())
				.build();
	}

	public static ApiResponse<Void> error(String errorCode, String message, Map<String, String> errors) {
		return ApiResponse.<Void>builder()
				.success(false)
				.errorCode(errorCode)
				.message(message)
				.errors(errors)
				.timestamp(OffsetDateTime.now())
				.build();
	}
}
