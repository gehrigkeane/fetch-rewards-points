package com.gehrig.fetch.points.domain;

import com.gehrig.fetch.points.dto.RequestPointAddition;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

@Value
public class PointEvent {

	public static final Comparator<PointEvent> ORDERING =
		Comparator.comparing(PointEvent::getEpoch)
			.thenComparing(PointEvent::getNano)
			.thenComparing(PointEvent::getUuid);

	@NotBlank(message = "payer must not be null or blank")
	String payer;

	// Heck, why not support ~2^63 points!
	@NotNull(message = "points must not be null")
	Long points;

	/**
	 * epoch, nano, and uuid are used to maintain payer order chronologically
	 * - first by time in epoch millis
	 * - second by time in nanos
	 * - third by UUID natural ordering if in the slim chance two payers have the same epoch + nano values
	 */
	@NotNull
	Long epoch;

	@NotNull
	Integer nano;

	@NotNull
	UUID uuid;

	public PointEvent(final String payer, final Long points, final Long epoch, final Integer nano, UUID uuid) {
		this.payer = payer;
		this.points = points;
		this.epoch = epoch;
		this.nano = nano;
		this.uuid = uuid;
	}

	public static PointEvent fromRequest(final RequestPointAddition requestPointAddition) {
		final ZonedDateTime date = requestPointAddition.getDate();
		final Instant now = date == null ? Instant.now() : date.toInstant();
		return new PointEvent(
			requestPointAddition.getPayer(),
			requestPointAddition.getPoints(),
			now.toEpochMilli(),
			now.getNano(),
			UUID.randomUUID()
		);
	}

	public static PointEvent withPoints(final PointEvent pointEvent, final long points) {
		return new PointEvent(
			pointEvent.getPayer(),
			points,
			pointEvent.getEpoch(),
			pointEvent.getNano(),
			pointEvent.getUuid()
		);
	}

	public static PointEvent merge(final PointEvent l, final PointEvent r) {
		if (!l.getPayer().equals(r.getPayer())) {
			throw new IllegalArgumentException();
		}
		final PointEvent min = Collections.min(Arrays.asList(l, r), PointEvent.ORDERING);
		return new PointEvent(
			min.getPayer(),
			l.getPoints() + r.getPoints(),
			min.getEpoch(),
			min.getNano(),
			min.getUuid()
		);
	}
}
