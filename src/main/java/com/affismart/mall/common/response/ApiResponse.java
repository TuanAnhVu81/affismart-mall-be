package com.affismart.mall.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API envelope returned by AffiSmart Mall endpoints")
public record ApiResponse<T>(
		@Schema(description = "Whether the request was processed successfully", example = "true")
		boolean success,

		@Schema(description = "Human-readable result message", example = "Products retrieved successfully")
		String message,

		@Schema(description = "Machine-readable error code. Present only when success=false", example = "INVALID_INPUT")
		String errorCode,

		@Schema(description = "Response payload. Type varies by endpoint")
		T data,

		@Schema(description = "Field-level validation errors. Present only for invalid input")
		Map<String, String> errors,

		@Schema(description = "Server timestamp when the response was created")
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
