package com.affismart.mall.exception;

import com.affismart.mall.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

	private final ErrorCode errorCode;

	public AppException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public AppException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
