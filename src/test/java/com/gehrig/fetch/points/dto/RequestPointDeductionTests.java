package com.gehrig.fetch.points.dto;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Data
public class RequestPointDeductionTests {

	/*
		Test Validation annotations behave expectedly
	*/

	private static Validator validator;

	@BeforeAll
	static void setUp() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@ParameterizedTest
	@MethodSource("provideValidValues")
	public void Should_PassValidation_When_Valid(final RequestPointDeduction valid) {
		Set<ConstraintViolation<RequestPointDeduction>> violations = validator.validate(valid);
		assertTrue(violations.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("provideInvalidValues")
	public void Should_FailValidation_When_Invalid(final RequestPointDeduction invalid) {
		Set<ConstraintViolation<RequestPointDeduction>> violations = validator.validate(invalid);
		assertFalse(violations.isEmpty());
	}

	private static Stream<Arguments> provideValidValues() {
		return Stream.of(
			Arguments.of(new RequestPointDeduction(Long.MAX_VALUE)),
			Arguments.of(new RequestPointDeduction(1L)),
			Arguments.of(new RequestPointDeduction(5L))
		);
	}

	private static Stream<Arguments> provideInvalidValues() {
		return Stream.of(
			// points: @NotNull, @Min
			Arguments.of(new RequestPointDeduction(null)),
			Arguments.of(new RequestPointDeduction(0L)),
			Arguments.of(new RequestPointDeduction(-1L)),
			Arguments.of(new RequestPointDeduction(Long.MIN_VALUE))
		);
	}
}
