package com.gehrig.fetch.points.domain;

import com.gehrig.fetch.points.dto.RequestPointAddition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class PointEventTests {

	private static final String VALID_PAYER = "bob";
	private static final Long VALID_POINTS = 5L;
	private static final Long VALID_EPOCH = 123L;
	private static final Integer VALID_NANO = 321;
	private static final UUID VALID_UUID = UUID.randomUUID();
	private static final ZonedDateTime VALID_DATE = ZonedDateTime.now();

	private static final PointEvent VALID = new PointEvent(VALID_PAYER, VALID_POINTS, VALID_EPOCH, VALID_NANO, VALID_UUID);

	/*
		Test Validation annotations behave expectedly
	*/

	private static Validator validator;

	@BeforeAll
	static void setUp() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	public void Should_PassValidation_When_Valid() {
		Set<ConstraintViolation<PointEvent>> violations = validator.validate(VALID);
		assertTrue(violations.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("provideInvalidPointEvents")
	public void Should_FailValidation_When_Invalid(final PointEvent invalid) {
		Set<ConstraintViolation<PointEvent>> violations = validator.validate(invalid);
		assertFalse(violations.isEmpty());
	}


	private static Stream<Arguments> provideInvalidPointEvents() {
		return Stream.of(
			// payer: @NotNull, @NotBlank
			Arguments.of(new PointEvent(null, VALID_POINTS, VALID_EPOCH, VALID_NANO, VALID_UUID)),
			Arguments.of(new PointEvent("  \t", VALID_POINTS, VALID_EPOCH, VALID_NANO, VALID_UUID)),
			// points: @NotNull
			Arguments.of(new PointEvent(VALID_PAYER, null, VALID_EPOCH, VALID_NANO, VALID_UUID)),
			// epoch: @NotNull
			Arguments.of(new PointEvent(VALID_PAYER, VALID_POINTS, null, VALID_NANO, VALID_UUID)),
			// nano: @NotNull
			Arguments.of(new PointEvent(VALID_PAYER, VALID_POINTS, VALID_EPOCH, null, VALID_UUID)),
			// uuid: @NotNull
			Arguments.of(new PointEvent(VALID_PAYER, VALID_POINTS, VALID_EPOCH, VALID_NANO, null))
		);
	}

	/*
		Test PointEvent.fromRequest behaves expectedly
	*/

	@Test
	public void Should_HaveEpochAndNanoValues_When_fromRequestHasNullDate() {
		final RequestPointAddition r = new RequestPointAddition(VALID_PAYER, VALID_POINTS, null);

		final PointEvent p = PointEvent.fromRequest(r);
		assertNotNull(p.getEpoch());
		assertNotNull(p.getNano());
	}

	@Test
	public void Should_ConvertDateToEpochAndNanoValues_When_fromRequestHasDate() {
		final RequestPointAddition r = new RequestPointAddition(VALID_PAYER, VALID_POINTS, VALID_DATE);
		final Instant instant = VALID_DATE.toInstant();
		final Long epoch = instant.toEpochMilli();
		final Integer nano = instant.getNano();

		final PointEvent p = PointEvent.fromRequest(r);
		assertEquals(epoch, p.getEpoch());
		assertEquals(nano, p.getNano());
	}

	/*
		Test PointEvent.withPoints behaves expectedly
	*/

	@Test
	public void Should_ConstructNewPointEvent_When_withPointsCalled() {
		final long newPoints = VALID_POINTS * 2;
		final PointEvent p = PointEvent.withPoints(VALID, newPoints);

		// Ensure all but points are equal
		assertEquals(newPoints, p.getPoints());
		assertEquals(VALID.getPayer(), p.getPayer());
		assertEquals(VALID.getEpoch(), p.getEpoch());
		assertEquals(VALID.getNano(), p.getNano());
		assertEquals(VALID.getUuid(), p.getUuid());

		// Ensure points differ from original
		assertNotEquals(newPoints, VALID.getPoints());
		assertNotEquals(VALID, p);
	}

	/*
		Test PointEvent.merge behaves expectedly
	*/

	@Test
	public void Should_ThrowIllegalArgumentException_When_mergePayersNotEqual() {
		final PointEvent l = new PointEvent("bob", VALID_POINTS, VALID_EPOCH, VALID_NANO, VALID_UUID);
		final PointEvent r = new PointEvent("sam", VALID_POINTS, VALID_EPOCH, VALID_NANO, VALID_UUID);

		assertThrows(IllegalArgumentException.class, () -> PointEvent.merge(l, r));
	}

	@Test
	public void Should_SumPoints_When_mergeCalled() {
		PointEvent l = new PointEvent(VALID_PAYER, 2L, VALID_EPOCH, VALID_NANO, VALID_UUID);
		PointEvent r = new PointEvent(VALID_PAYER, 3L, VALID_EPOCH, VALID_NANO, VALID_UUID);
		PointEvent merged = PointEvent.merge(l, r);
		assertEquals(5L, merged.getPoints());

		// Test negative operand
		l = new PointEvent(VALID_PAYER, -10L, VALID_EPOCH, VALID_NANO, VALID_UUID);
		r = new PointEvent(VALID_PAYER, 5L, VALID_EPOCH, VALID_NANO, VALID_UUID);
		merged = PointEvent.merge(l, r);
		assertEquals(-5L, merged.getPoints());
	}


	@Test
	public void Should_ConsistentlyMinimizeTimeFields_When_mergeCalled() {
		final UUID greaterUUID = UUID.randomUUID();

		// Test l operand
		PointEvent l = new PointEvent(VALID_PAYER, 2L, VALID_EPOCH, VALID_NANO, VALID_UUID);
		PointEvent r = new PointEvent(VALID_PAYER, 3L, VALID_EPOCH+1, VALID_NANO+1, greaterUUID);
		PointEvent merged = PointEvent.merge(l, r);
		assertEquals(VALID_EPOCH, merged.getEpoch());
		assertEquals(VALID_NANO, merged.getNano());
		assertEquals(VALID_UUID, merged.getUuid());

		// Test r operand
		l = new PointEvent(VALID_PAYER, 2L, VALID_EPOCH+1, VALID_NANO+1, greaterUUID);
		r = new PointEvent(VALID_PAYER, 3L, VALID_EPOCH, VALID_NANO, VALID_UUID);
		merged = PointEvent.merge(l, r);
		assertEquals(VALID_EPOCH, merged.getEpoch());
		assertEquals(VALID_NANO, merged.getNano());
		assertEquals(VALID_UUID, merged.getUuid());
	}

}
