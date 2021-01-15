package com.gehrig.fetch.points.domain;

import com.gehrig.fetch.points.exception.InvalidDeductionException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class is the core to the domain logic of this Web Application.
 * Semantically, it book-keeps payment events made to a User on behalf of payers.
 *
 * We've settled on the implementation below involving an ordered collection of
 * payment events and segregated totals for several reasons:
 *
 * 	1.	The ordered collection of payment events both inserts and iterates across
 * 			payments events in a performant manner.
 * 			Specifically, the TreeSet handles insertions in O(log(n)) which seems
 * 			reasonable considering we get an in-order iteration of O(n).
 * 			Consequently, points deductions are satisfactorily timely given an O(n)
 * 			iteration and O(log(n)) reinsertion of modified events.
 *
 * 	2.	Segregated User and payers totals provide a couple benefits that
 * 			justified the added book-keeping complexity. Negative balance heuristics
 * 		 	are exceptionally timely (O(1)), and payer order is preserved even
 * 		 	withstanding removal of original payment events. Additionally, decoupling
 * 		 	totals from the pointEvents data structure greatly reduces algorithmic
 * 		 	complexity and readability.
 *
 * Finally, notice the coordinated synchronization between the add and delete
 * methods. Due to the problems complexity, and difficulty consolidating totals
 * with payment events into a data structure with concurrency guarantees we've
 * synchronized point addition and deletion. This should protect us from the
 * threaded runtime of the webserver in the event of high request throughput.
 *
 */
public class UserPoints {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserPoints.class);
	private static final String INVALID_PAYER_POINTS = "Failed to add [%s, %d] to user `%s`, negative payer balance(s) are prohibited";
	private static final String INVALID_USER_POINTS = "Failed to deduct %d points from user `%s`, negative user balance(s) are prohibited";

	@Getter
	private final String user;
	private final Object lock = new Object();
	private final Set<PointEvent> pointEvents = new TreeSet<>(PointEvent.ORDERING);
	private final Map<String, PointEvent> payerTotals = new HashMap<>();
	private long userTotal;

	public UserPoints(final String user) {
		this.user = user;
		this.userTotal = 0L;
	}

	/*
		The following comprises the 'brains' of point deduction
	*/

	/**
	 * Deduct points, either from a payers balance or else the entire Users balance from oldest to newest points.
	 * @param payer An potentially Null payer name, used to solely deduct points from an payer
	 * @param points An points value for which to deduct from a payer or else this User
	 * @return An ordered list of removed points from oldest to newest
	 */
	private List<PointEvent> deletePoints(@Nullable final String payer, final Long points) {
		PointEvent updated = null;
		List<PointEvent> removed = new ArrayList<>(); // profiling may help determine better starting size
		Iterator<PointEvent> iter = this.pointEvents.iterator();

		// The algorithm anticipates positive points for deduction
		// Thus, if they're negative invert for correct behavior (addPoints likely submits negative points)
		long pointsToDeduct = points > 0 ? points : -points;

		while (iter.hasNext() && pointsToDeduct > 0) {
			final PointEvent event = iter.next();
			final long payerDeduction;

			if (payer != null && !event.getPayer().equals(payer)) {
				// if payer name is specified, impertinent payers aren't considered
				continue;
			} else if (event.getPoints() - pointsToDeduct <= 0) {
				// This PointEvent is 0'd: remove it, update point deduction balances, add to removed
				iter.remove();

				/*
					We've zeroed this event
					Thus:
				   - adjust payer balance by full event points
				   - Add removed item with full event points
				*/
				payerDeduction = event.getPoints();
				removed.add(PointEvent.withPoints(event, -event.getPoints()));

				pointsToDeduct -= event.getPoints();
			} else {
				// event must be updated: remove it and compute replacement (loop is effectively finished)
				iter.remove();

				/*
					We've partially deducted points from this event, this only happens when event.points > pointsToDeduct
				  Thus:
				   - adjust payer balance by pointsToDeduct
				   - Add removed item with points deducted from this event
				   - Populated updated event with remaining points
				*/
				payerDeduction = pointsToDeduct;
				removed.add(PointEvent.withPoints(event, -pointsToDeduct));
				updated = PointEvent.withPoints(event, event.getPoints() - pointsToDeduct);

				// This case is terminal, consequently zero pointsToDeduct
				pointsToDeduct -= pointsToDeduct;
			}

			// update payer total, we're relatively certain unboxing v is null safe
			this.payerTotals.compute(event.getPayer(), (k, v) -> PointEvent.withPoints(v, v.getPoints() - payerDeduction));
		}

		this.userTotal -= points;
		// if iteration ended evenly and no PointEvents required modification then updated is null
		if (updated != null) {
			pointEvents.add(updated);
		}
		LOGGER.info("Deducted {} points from {}", points, this.user);
		return removed;
	}

	/*
		Public Methods
	*/

	/**
	 * Add points to this Users balance.
	 * @param pointEvent An event comprised of the payer, points, and date of payment.
	 */
	public void addPoints(final PointEvent pointEvent) {
		synchronized (lock) {

			final String payer = pointEvent.getPayer();
			final Long points = pointEvent.getPoints();

			// This is an odd edge-case, but we're going to assume additions of 0 points can be thrown away
			if (points == 0) return;

			// If points are positive, we can safely update totals + events and be done
			if (points > 0) {
				this.userTotal += points;
				this.payerTotals.compute(payer, (k, v) -> (v == null) ? pointEvent : PointEvent.merge(v, pointEvent));
				this.pointEvents.add(pointEvent);
				LOGGER.info("Added {} to {}", pointEvent, this.user);
				return;
			}

			final Long payerTotal = payerTotals.getOrDefault(payer, PointEvent.withPoints(pointEvent, 0)).getPoints();
			// Vet points against user and payer totals, neither user nor payer may have negative values
			if (this.userTotal + points < 0 || payerTotal + points < 0) {
				final String details = INVALID_PAYER_POINTS.formatted(payer, pointEvent.getPoints(), this.user);
				LOGGER.error(details);
				throw new InvalidDeductionException(details);
			}

			this.deletePoints(payer, points);

			LOGGER.info("Deducted {} from {}", pointEvent, this.user);
		}
	}

	/**
	 * Deduct points from this Users balance from oldest to newest points.
	 * @param points An points value for which to deduct from this Users balance
	 * @return An ordered list of removed points from oldest to newest
	 */
	public List<PointEvent> deletePoints(final Long points) {
		synchronized (lock) {
			// Vet points against user total, user may not have negative total
			if (this.userTotal - points < 0) {
				final String details = INVALID_USER_POINTS.formatted(points, this.user);
				LOGGER.error(details);
				throw new InvalidDeductionException(details);
			}

			return this.deletePoints(null, points);
		}
	}

	/**
	 * Compute and return an ordered list of payer totals.
	 *
	 * Specifically, we leverage the PointEvent.merge method to conveniently
	 * sum payer events while minimizing the payer time data, thus allowing a
	 * reasonably quick sort before result are returned.
	 *
	 * @return An ordered list of PointEvent's whose points comprise payer totals.
	 */
	public List<PointEvent> getPoints() {
		final List<PointEvent> payerTotals = new ArrayList<>(this.payerTotals.values().stream()
			/*
				Reduce pointsEvents s.t. result is map of (payer name) -> PointsEvent
				 i.e.
					 {
						 DANNON -> PointsEvent(DANNON, 1000, ...),
						 UNILEVER -> PointsEvent(UNILEVER, 0, ...),
						 MILLER -> PointsEvent(MILLER, 5300, ...),
					 }

				Note: ordering below relies on PointEvent.merge minimizing time data
			*/
			.collect(Collectors.toMap(PointEvent::getPayer, Function.identity(), PointEvent::merge))
			.values()
		);

		payerTotals.sort(PointEvent.ORDERING);
		return payerTotals;
	}
}
