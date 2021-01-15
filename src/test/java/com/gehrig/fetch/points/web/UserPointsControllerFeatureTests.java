package com.gehrig.fetch.points.web;

import com.gehrig.fetch.points.service.UserPointsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.NestedServletException;

import javax.validation.ConstraintViolationException;
import java.math.BigInteger;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {UserPointsController.class, UserPointsService.class})
@WebMvcTest
public class UserPointsControllerFeatureTests {

	private static final String USER = "bob";
	private static final String URI = "/user/" + USER + "/points";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserPointsService userPointsService;

	@BeforeEach
	public void resetUserPointsService() {
		ReflectionTestUtils.invokeMethod(userPointsService, "resetUsers");
	}

	/*
		Base Case Tests
	*/

	@Test
	public void GetPoints_Should_ReturnHTTP200AndEmptyContent_When_UserHasNoPoints() throws Exception {
		final var responseBody = this.queryGetPointsResponseBody(URI, status().isOk());
		final var expectedBody = "[]";
		assertThat(responseBody).isEqualTo(expectedBody);
	}

	@Test
	public void PostPoints_Should_ReturnHTTP200_When_PointsPosted() throws Exception {
		final var requestBody = this.toJSON("{'payer': 'A', 'points': 100}");

		this.queryAddPoints(URI, requestBody, status().isOk());
	}

	/*
		Feature Test 1 - Problem Statement Example
	*/

	@Test
	public void Application_Should_SatisfyProblemExample_When_Queried() throws Exception {
		final var points = Arrays.asList(
			this.toJSON("{'payer': 'DANNON', 'points': 300, 'date': '2020-10-31T10:00:00'}"),
			this.toJSON("{'payer': 'UNILEVER', 'points': 200, 'date': '2020-10-31T11:00:00'}"),
			this.toJSON("{'payer': 'DANNON', 'points': -200, 'date': '2020-10-31T15:00:00'}"),
			this.toJSON("{'payer': 'MILLER COORS', 'points': 10000, 'date': '2020-11-01T14:00:00'}"),
			this.toJSON("{'payer': 'DANNON', 'points': 1000, 'date': '2020-11-02T14:00:00'}")
		);

		// Post all the points
		for (final String point : points) {
			this.queryAddPoints(URI, point, status().isOk());
		}

		// Intermediate Get
		var responseBody = queryGetPointsResponseBody(URI, status().isOk());
		var expectedBody = this.toJSON("""
				[
					{'payer':'DANNON','points':1100},
					{'payer':'UNILEVER','points':200},
					{'payer':'MILLER COORS','points':10000}
				]
			""");
		assertThat(responseBody).isEqualTo(expectedBody);

		// Delete
		var requestBody = this.toJSON("{'points': 5000}");
		responseBody = this.queryDeletePointsResponseBody(URI, requestBody, status().isOk());
		expectedBody = this.toJSON("""
				[
					{'payer':'DANNON','points':-100},
					{'payer':'UNILEVER','points':-200},
					{'payer':'MILLER COORS','points':-4700}
				]
			""");
		assertThat(responseBody).isEqualTo(expectedBody);

		// Final Get
		responseBody = this.queryGetPointsResponseBody(URI, status().isOk());
		expectedBody = this.toJSON("""
				[
					{'payer':'DANNON','points':1000},
					{'payer':'UNILEVER','points':0},
					{'payer':'MILLER COORS','points':5300}
				]
			""");
		assertThat(responseBody).isEqualTo(expectedBody);
	}

	/*
		Feature Test 2 - Zero Balances with Post Request
	*/

	@Test
	public void Application_Should_CorrectlyZeroBalances_When_NegativePointsPosted() throws Exception {
		final var points = Arrays.asList(
			this.toJSON("{'payer': 'A', 'points': 111}"),
			this.toJSON("{'payer': 'B', 'points': 222}"),
			this.toJSON("{'payer': 'C', 'points': 333}"),
			this.toJSON("{'payer': 'A', 'points': 0}"),
			this.toJSON("{'payer': 'B', 'points': 0}"),
			this.toJSON("{'payer': 'C', 'points': 0}"),
			this.toJSON("{'payer': 'A', 'points': -111}"),
			this.toJSON("{'payer': 'B', 'points': -222}"),
			this.toJSON("{'payer': 'C', 'points': -333}")
		);

		// Post all the points
		for (final String point : points) {
			this.queryAddPoints(URI, point, status().isOk());
		}

		// Get - Ensure insertion order is maintained
		var responseBody = queryGetPointsResponseBody(URI, status().isOk());
		var expectedBody = this.toJSON("""
				[
					{'payer':'A','points':0},
					{'payer':'B','points':0},
					{'payer':'C','points':0}
				]
			""");
		assertThat(responseBody).isEqualTo(expectedBody);
	}

	/*
		Feature Test 3 - Zero Balances with Delete Request
	*/

	@Test
	public void Application_Should_CorrectlyZeroBalances_When_PointsDeleted() throws Exception {
		final var points = Arrays.asList(
			this.toJSON("{'payer': 'A', 'points': 111}"),
			this.toJSON("{'payer': 'B', 'points': 222}"),
			this.toJSON("{'payer': 'C', 'points': 333}"),
			this.toJSON("{'payer': 'A', 'points': 0}"),
			this.toJSON("{'payer': 'B', 'points': 0}"),
			this.toJSON("{'payer': 'C', 'points': 0}"),
			this.toJSON("{'payer': 'A', 'points': 1000}"),
			this.toJSON("{'payer': 'B', 'points': 1000}"),
			this.toJSON("{'payer': 'C', 'points': 1000}"),
			this.toJSON("{'payer': 'A', 'points': -200}"),
			this.toJSON("{'payer': 'B', 'points': -300}"),
			this.toJSON("{'payer': 'C', 'points': -400}")
		);

		// Post all the points
		for (final String point : points) {
			this.queryAddPoints(URI, point, status().isOk());
		}

		// Intermediate Get
		var responseBody = queryGetPointsResponseBody(URI, status().isOk());
		var expectedBody = this.toJSON("[{'payer':'A','points':911},{'payer':'B','points':922},{'payer':'C','points':933}]");
		assertThat(responseBody).isEqualTo(expectedBody);

		// Delete
		var requestBody = this.toJSON("{'points': 2766}");
		responseBody = this.queryDeletePointsResponseBody(URI, requestBody, status().isOk());
		expectedBody = this.toJSON("[{'payer':'A','points':-911},{'payer':'B','points':-922},{'payer':'C','points':-933}]");
		assertThat(responseBody).isEqualTo(expectedBody);

		// Final Get
		responseBody = queryGetPointsResponseBody(URI, status().isOk());
		expectedBody = this.toJSON("[{'payer':'A','points':0},{'payer':'B','points':0},{'payer':'C','points':0}]");
		assertThat(responseBody).isEqualTo(expectedBody);
	}

	/*
		Feature Test 4 - Ensure Delete Returns Ordered Deduction (As opposed to one deduction per payer)
	*/

	@Test
	public void Application_Should_ReturnCorrectDeletionEvents_When_PointsDeleted() throws Exception {
		final var points = Arrays.asList(
			this.toJSON("{'payer': 'A', 'points': 111}"),
			this.toJSON("{'payer': 'B', 'points': 222}"),
			this.toJSON("{'payer': 'C', 'points': 333}"),
			this.toJSON("{'payer': 'A', 'points': 0}"),
			this.toJSON("{'payer': 'B', 'points': 0}"),
			this.toJSON("{'payer': 'C', 'points': 0}"),
			this.toJSON("{'payer': 'A', 'points': 1000}"),
			this.toJSON("{'payer': 'B', 'points': 1000}"),
			this.toJSON("{'payer': 'C', 'points': 1000}")
		);

		// Post all the points
		for (final String point : points) {
			this.queryAddPoints(URI, point, status().isOk());
		}

		// Intermediate Get
		var responseBody = queryGetPointsResponseBody(URI, status().isOk());
		var expectedBody = this.toJSON("""
				[
					{'payer':'A','points':1111},
					{'payer':'B','points':1222},
					{'payer':'C','points':1333}
				]
			""");
		assertThat(responseBody).isEqualTo(expectedBody);

		// Delete
		var requestBody = this.toJSON("{'points': 3666}");
		responseBody = this.queryDeletePointsResponseBody(URI, requestBody, status().isOk());
		expectedBody = this.toJSON("""
				[
					{'payer':'A','points':-111},
					{'payer':'B','points':-222},
					{'payer':'C','points':-333},
					{'payer':'A','points':-1000},
					{'payer':'B','points':-1000},
					{'payer':'C','points':-1000}
				]
			""");
		assertThat(responseBody).isEqualTo(expectedBody);

		// Final Get
		responseBody = queryGetPointsResponseBody(URI, status().isOk());
		expectedBody = this.toJSON("""
				[
					{'payer':'A','points':0},
					{'payer':'B','points':0},
					{'payer':'C','points':0}
				]
			""");
		assertThat(responseBody).isEqualTo(expectedBody);
	}


	/*
		Feature Test 5 - Ensure order is maintained with out-of-order additions
	*/

	@Test
	public void Application_Should_MaintainOrderOfEvents_When_PointsPostedOutOfOrder() throws Exception {
		final var points = Arrays.asList(
			this.toJSON("{'payer': 'A', 'points': 30, 'date': '2020-01-03T00:00:00.000Z'}"),
			this.toJSON("{'payer': 'A', 'points': 10, 'date': '2020-01-01T00:00:00'}"),
			this.toJSON("{'payer': 'A', 'points': 20, 'date': '2020-01-02'}"),
			this.toJSON("{'payer': 'C', 'points': 100, 'date': '1945-01-01T01:00:00.000MST'}"),
			this.toJSON("{'payer': 'B', 'points': 2, 'date': '2020-01-02T08:00:00'}"),
			this.toJSON("{'payer': 'B', 'points': 3, 'date': '2020-01-03T09:00:00'}"),
			this.toJSON("{'payer': 'B', 'points': 1, 'date': '2020-01-01T22:00:00.000Z'}")
		);

		// Post all the points
		for (final String point : points) {
			this.queryAddPoints(URI, point, status().isOk());
		}

		// Intermediate Get
		var responseBody = queryGetPointsResponseBody(URI, status().isOk());
		var expectedBody = this.toJSON("""
				[
					{'payer':'C','points':100},
					{'payer':'A','points':60},
					{'payer':'B','points':6}
				]
			""");
		assertThat(responseBody).isEqualTo(expectedBody);

		// Delete
		var requestBody = this.toJSON("{'points': 166}");
		responseBody = this.queryDeletePointsResponseBody(URI, requestBody, status().isOk());
		expectedBody = this.toJSON("""
				[
					{'payer':'C','points':-100},
					{'payer':'A','points':-10},
					{'payer':'B','points':-1},
					{'payer':'A','points':-20},
					{'payer':'B','points':-2},
					{'payer':'A','points':-30},
					{'payer':'B','points':-3}
				]
			""");
		assertThat(responseBody).isEqualTo(expectedBody);

		// Final Get
		responseBody = queryGetPointsResponseBody(URI, status().isOk());
		expectedBody = this.toJSON("""
				[
					{'payer':'C','points':0},
					{'payer':'A','points':0},
					{'payer':'B','points':0}
				]
			""");
		assertThat(responseBody).isEqualTo(expectedBody);
	}

	/*
		Sad Path Tests
	*/

	@Test
	public void PostPoints_Should_ReturnHTTP400_When_ResultWouldBeNegativePayerBalance() throws Exception {
		// Test base case
		var requestBody = this.toJSON("{'payer': 'A', 'points': -100}");
		this.queryAddPoints(URI, requestBody, status().isBadRequest());

		// Test negative addition with non-zero balance
		requestBody = this.toJSON("{'payer': 'A', 'points': 100}");
		this.queryAddPoints(URI, requestBody, status().isOk());

		requestBody = this.toJSON("{'payer': 'A', 'points': -101}");
		this.queryAddPoints(URI, requestBody, status().isBadRequest());
	}

	@Test
	public void DeletePoints_Should_ReturnHTTP400_When_ResultWouldBeNegativeUserBalance() throws Exception {
		// Test base case
		var requestBody = this.toJSON("{'points': -1}");
		this.queryDeletePointsResponseBody(URI, requestBody, status().isBadRequest());

		// Test delete with non-zero balance
		var requestBodies = Arrays.asList(
			this.toJSON("{'payer': 'A', 'points': 1}"),
			this.toJSON("{'payer': 'A', 'points': 2}"),
			this.toJSON("{'payer': 'A', 'points': 3}")
		);

		for (final String rb : requestBodies) {
			this.queryAddPoints(URI, rb, status().isOk());
		}

		requestBody = this.toJSON("{'points': 7}");
		this.queryDeletePointsResponseBody(URI, requestBody, status().isBadRequest());
	}

	/*
		Input Validation Tests
	*/

	@Test
	public void AllRoutes_Should_ReturnHTTP400_When_UserNameIsBlank() throws Exception {
		final var blankUserURI = "/user/  /points";

		// These are unfortunately messy, but they work FTTB
		assertThrows(ConstraintViolationException.class, () -> {
			try {
				this.queryGetPointsResponseBody(blankUserURI, status().isBadRequest());
			} catch (NestedServletException e) {
				throw e.getCause();
			}
  	});

		final var requestBody = this.toJSON("{'payer': 'A', 'points': 100}");
		assertThrows(ConstraintViolationException.class, () -> {
			try {
				this.queryAddPoints(blankUserURI, requestBody, status().isBadRequest());
			} catch (NestedServletException e) {
				throw e.getCause();
			}
  	});

		final var requestBody2 = this.toJSON("{'points': 0}");
		this.queryDeletePointsResponseBody(blankUserURI, requestBody2, status().isBadRequest());
	}

	@Test
	public void PostPoints_Should_ReturnHTTP400_When_RequestBodyIsInvalid() throws Exception {
		final var biggerThanLong = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(Long.MAX_VALUE)).toString();

		var invalidRequestBodies = Arrays.asList(
			// invalid JSON
			this.toJSON("{'payer': null, 'points': 100}"),
			// payer: Null, Blank, Numeric, >63 characters
			this.toJSON("{'payer': null, 'points': 100}"),
			this.toJSON("{'payer': '',   'points': 100}"),
			this.toJSON("{'payer': '  ', 'points': 100}"),
			this.toJSON("{'payer': 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'points': 100}"),
			// points: Null, Alphabetic, >Long.MAX_VALUE
			this.toJSON("{'payer': 'bob', 'points': null}"),
			this.toJSON("{'payer': 'bob', 'points': 'abc'}"),
			this.toJSON("{'payer': 'bob', 'points': " + biggerThanLong + "}"),
			// date: invalid/un-parsable dates, only Zoned ISO-8601 are permitted atm
			this.toJSON("{'payer': 'bob', 'points': 100, 'date': '2021-01'}"),
			this.toJSON("{'payer': 'bob', 'points': 100, 'date': '2021-01-01T'}"),
			this.toJSON("{'payer': 'bob', 'points': 100, 'date': '0000-00-00T00:00:00.000Z'}")
		);

		for (final String invalidRequestBody : invalidRequestBodies) {
			this.queryAddPoints(URI, invalidRequestBody, status().isBadRequest());
		}
	}

	@Test
	public void DeletePoints_Should_ReturnHTTP400_When_RequestBodyIsInvalid() throws Exception {
		final var biggerThanLong = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(Long.MAX_VALUE)).toString();

		var invalidRequestBodies = Arrays.asList(
			// invalid JSON
			this.toJSON("{'payer': null, 'points': 100}"),
			// payer: Null, Blank, Numeric, >63 characters
			this.toJSON("{'payer': null, 'points': 100}"),
			this.toJSON("{'payer': '',   'points': 100}"),
			this.toJSON("{'payer': '  ', 'points': 100}"),
			this.toJSON("{'payer': 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'points': 100}"),
			// points: Null, Alphabetic, >Long.MAX_VALUE
			this.toJSON("{'payer': 'bob', 'points': null}"),
			this.toJSON("{'payer': 'bob', 'points': 'abc'}"),
			this.toJSON("{'payer': 'bob', 'points': " + biggerThanLong + "}"),
			this.toJSON("{'payer': 'bob', 'points': -1}")
		);

		for (final String invalidRequestBody : invalidRequestBodies) {
			this.queryAddPoints(URI, invalidRequestBody, status().isBadRequest());
		}
	}

	/*
		Utility Methods
	*/

	private String queryGetPointsResponseBody(final String uri, final ResultMatcher status) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.get(uri))
			.andExpect(status)
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private void queryAddPoints(final String uri, final String requestBody, final ResultMatcher status) throws Exception {
		mockMvc.perform(
			MockMvcRequestBuilders.post(uri)
				.content(requestBody)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status)
			.andReturn()
			;
	}

	private String queryDeletePointsResponseBody(final String uri, final String requestBody, final ResultMatcher status) throws Exception {
		return mockMvc.perform(
			MockMvcRequestBuilders.delete(uri)
				.content(requestBody)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status)
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private String toJSON(final String json) {
		return json.replaceAll("\\s+", "").replaceAll("'", "\"");
	}
}
