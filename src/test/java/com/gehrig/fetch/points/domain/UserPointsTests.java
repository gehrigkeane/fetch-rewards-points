package com.gehrig.fetch.points.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class UserPointsTests {

	private static final String USER = "bob";

	private UserPoints userPoints;

	@BeforeEach
	public void resetUserPoints() {
		this.userPoints = new UserPoints(USER);
	}

	@Test
	public void Should_SatisfyProblemExample_WhenCalled() {
		final var p0 = new PointEvent("DANNON", 300L, 0L, 0, UUID.randomUUID());
		final var p1 = new PointEvent("UNILEVER", 200L, 1L, 0, UUID.randomUUID());
		final var p2 = new PointEvent("DANNON", -200L, 2L, 0, UUID.randomUUID());
		final var p3 = new PointEvent("MILLER COORS", 10_000L, 3L, 0, UUID.randomUUID());
		final var p4 = new PointEvent("DANNON", 1_000L, 4L, 0, UUID.randomUUID());

		// Add points
		for (final PointEvent pointEvent : Arrays.asList(p0, p1, p2, p3, p4)) {
			this.userPoints.addPoints(pointEvent);
		}

		// Intermediate get points
		var getResult = this.userPoints.getPoints();
		assertThat(getResult).hasSize(3);
		assertThat(getResult)
			.extracting("payer", "points")
			.contains(
				tuple("DANNON", 1_100L),
				tuple("UNILEVER", 200L),
				tuple("MILLER COORS", 10_000L)
			);

		var deleteResult = this.userPoints.deletePoints(5000L);
		assertThat(deleteResult).hasSize(3);
		assertThat(deleteResult)
			.extracting("payer", "points")
			.contains(
				tuple("DANNON", -100L),
				tuple("UNILEVER", -200L),
				tuple("MILLER COORS", -4_700L)
			);

		getResult = this.userPoints.getPoints();
		assertThat(getResult).hasSize(3);
		assertThat(getResult)
			.extracting("payer", "points")
			.contains(
				tuple("DANNON", 1_000L),
				tuple("UNILEVER", 0L),
				tuple("MILLER COORS", 5_300L)
			);
	}
}
