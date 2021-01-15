package com.gehrig.fetch.points.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidDeductionException extends RuntimeException {
	public InvalidDeductionException() {
		super();
	}

	public InvalidDeductionException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidDeductionException(String message) {
		super(message);
	}

	public InvalidDeductionException(Throwable cause) {
		super(cause);
	}
}
