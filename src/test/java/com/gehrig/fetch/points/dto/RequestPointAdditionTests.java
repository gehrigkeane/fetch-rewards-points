package com.gehrig.fetch.points.dto;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Data
public class RequestPointAdditionTests {

	private static final String VALID_PAYER = "bob";
	private static final Long VALID_POINTS = 5L;
	private static final ZonedDateTime VALID_DATE = ZonedDateTime.now();

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
	public void Should_PassValidation_When_Valid(final RequestPointAddition valid) {
		Set<ConstraintViolation<RequestPointAddition>> violations = validator.validate(valid);
		assertTrue(violations.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("provideInvalidValues")
	public void Should_FailValidation_When_Invalid(final RequestPointAddition invalid) {
		Set<ConstraintViolation<RequestPointAddition>> violations = validator.validate(invalid);
		assertFalse(violations.isEmpty());
	}

	private static Stream<Arguments> provideValidValues() {
		return Stream.of(
			// payer
			Arguments.of(new RequestPointAddition("a", VALID_POINTS, VALID_DATE)),
			Arguments.of(new RequestPointAddition("😀", VALID_POINTS, VALID_DATE)),
			Arguments.of(new RequestPointAddition("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", VALID_POINTS, VALID_DATE)),
			// points
			Arguments.of(new RequestPointAddition(VALID_PAYER, Long.MAX_VALUE, VALID_DATE)),
			Arguments.of(new RequestPointAddition(VALID_PAYER, 0L, VALID_DATE)),
			Arguments.of(new RequestPointAddition(VALID_PAYER, Long.MIN_VALUE, VALID_DATE)),
			// date
			Arguments.of(new RequestPointAddition(VALID_PAYER, VALID_POINTS, ZonedDateTime.of(LocalDateTime.MAX, ZoneOffset.UTC))),
			Arguments.of(new RequestPointAddition(VALID_PAYER, VALID_POINTS, ZonedDateTime.of(LocalDateTime.of(1940, 7, 13, 0, 0), ZoneOffset.UTC))),
			Arguments.of(new RequestPointAddition(VALID_PAYER, VALID_POINTS, ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.UTC)))
		);
	}

	private static Stream<Arguments> provideInvalidValues() {
		return Stream.of(
			// payer: @NotNull, @NotBlank, @Size > 63
			Arguments.of(new RequestPointAddition(null, VALID_POINTS, VALID_DATE)),
			Arguments.of(new RequestPointAddition("  \t", VALID_POINTS, VALID_DATE)),
			Arguments.of(new RequestPointAddition("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", VALID_POINTS, VALID_DATE)),
			// points: @NotNull
			Arguments.of(new RequestPointAddition(VALID_PAYER, null, VALID_DATE))
		);
	}
}
