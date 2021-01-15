package com.gehrig.fetch.points.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestControllerAdvice
public class ValidationExceptionHandler extends ResponseEntityExceptionHandler {

	// Handle @Validate Errors (Path Variable and Request Parameter validation)
	@ExceptionHandler(ConstraintViolationException.class)
	public void constraintViolationException(HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.BAD_REQUEST.value());
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", new Date());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("message", ex.getMessage());
		body.put("path", ((ServletWebRequest) request).getRequest().getRequestURI());
		return this.handleExceptionInternal(ex, body, headers, status, request);
	}

	// Handle @Valid Errors
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
		MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request
	) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", new Date());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("path", ((ServletWebRequest) request).getRequest().getRequestURI());

		// Collect simple error message to List
		List<String> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(DefaultMessageSourceResolvable::getDefaultMessage)
			.collect(Collectors.toList());

		body.put("errors", errors);
		return new ResponseEntity<>(body, headers, status);
	}
}
