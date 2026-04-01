package com.affismart.mall.exception;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			errors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}

		return buildResponse(ErrorCode.INVALID_INPUT, errors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
			errors.put(violation.getPropertyPath().toString(), violation.getMessage());
		}

		return buildResponse(ErrorCode.INVALID_INPUT, errors);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
			MethodArgumentTypeMismatchException exception
	) {
		Map<String, String> errors = Map.of(
				exception.getName(),
				"Invalid value '" + exception.getValue() + "'"
		);
		return buildResponse(ErrorCode.INVALID_INPUT, errors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
		Map<String, String> errors = Map.of("request_body", "Malformed or unreadable JSON payload");
		return buildResponse(ErrorCode.INVALID_INPUT, errors);
	}

	@ExceptionHandler(AppException.class)
	public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		ApiResponse<Void> body = ApiResponse.error(errorCode.getCode(), exception.getMessage());
		return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException exception) {
		return buildResponse(ErrorCode.UNAUTHORIZED);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
		return buildResponse(ErrorCode.ACCESS_DENIED);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception exception) {
		log.error("Unhandled exception", exception);
		return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR);
	}

	private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode) {
		return ResponseEntity.status(errorCode.getHttpStatus())
				.body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
	}

	private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode, Map<String, String> errors) {
		return ResponseEntity.status(errorCode.getHttpStatus())
				.body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage(), errors));
	}
}
