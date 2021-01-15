package com.gehrig.fetch.points.dto;

import lombok.Value;
import org.junit.jupiter.api.BeforeAll;
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

@Value
public class ResponsePointTests {

	private static final String VALID_PAYER = "bob";
	private static final Long VALID_POINTS = 5L;

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
	public void Should_PassValidation_When_Valid(final ResponsePoint valid) {
		Set<ConstraintViolation<ResponsePoint>> violations = validator.validate(valid);
		assertTrue(violations.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("provideInvalidValues")
	public void Should_FailValidation_When_Invalid(final ResponsePoint invalid) {
		Set<ConstraintViolation<ResponsePoint>> violations = validator.validate(invalid);
		assertFalse(violations.isEmpty());
	}

	private static Stream<Arguments> provideValidValues() {
		return Stream.of(
			// payer
			Arguments.of(new ResponsePoint("a", VALID_POINTS)),
			Arguments.of(new ResponsePoint("😀", VALID_POINTS)),
			// points
			Arguments.of(new ResponsePoint(VALID_PAYER, Long.MAX_VALUE)),
			Arguments.of(new ResponsePoint(VALID_PAYER, 0L)),
			Arguments.of(new ResponsePoint(VALID_PAYER, Long.MIN_VALUE))
		);
	}

	private static Stream<Arguments> provideInvalidValues() {
		return Stream.of(
			// payer: @NotNull
			Arguments.of(new ResponsePoint(null, VALID_POINTS)),
			// points: @NotNull
			Arguments.of(new ResponsePoint(VALID_PAYER, null))
		);
	}
}
