package com.gehrig.fetch.points.web;

import com.gehrig.fetch.points.domain.PointEvent;
import com.gehrig.fetch.points.dto.RequestPointAddition;
import com.gehrig.fetch.points.dto.RequestPointDeduction;
import com.gehrig.fetch.points.dto.ResponsePoint;
import com.gehrig.fetch.points.service.UserPointsService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;


@Api("Endpoints that operate against the User resource: adding, deducting, or retrieving points for Users")
@RestController
@RequestMapping("/user/")
@Validated
public class UserPointsController {

	private final UserPointsService userPointsService;

	@Autowired
	public UserPointsController(UserPointsService userPointService) {
		this.userPointsService = userPointService;
	}

	@PostMapping(path = "/{name}/points", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Adds points (by payer) to a User, points may be negative")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Points were successfully added"),
		@ApiResponse(code = 400, message = "Either Path/Body validation failed, or points exceeded User-Payer balance"),
		@ApiResponse(code = 500, message = "Unexpected errors have occurred"),
	})
	public void postPoints(
		@ApiParam("The User name") @PathVariable("name") @NotBlank String name,
		@Valid @RequestBody RequestPointAddition requestPointAddition
	) {
		this.userPointsService.addPoints(name, PointEvent.fromRequest(requestPointAddition));
	}

	@GetMapping(path = "/{name}/points", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Retrieves a Users point totals aggregated by payer")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "User point totals retrieved successfully"),
		@ApiResponse(code = 400, message = "User name was malformed"),
		@ApiResponse(code = 500, message = "Unexpected errors have occurred"),
	})
	public List<ResponsePoint> getPoints(
		@ApiParam("The User name") @PathVariable("name") @NotBlank String name
	) {
		// Convert PointsEvents to PointsResponse removing cruft. Namely, epoch and uuid
		return this.userPointsService.getPoints(name)
			.stream()
			.map(pe -> new ResponsePoint(pe.getPayer(), pe.getPoints()))
			.collect(Collectors.toList());
	}

	@DeleteMapping(path = "/{name}/points", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Deducts points from a User, points are removed from oldest to newest")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Points deducted from User successfully"),
		@ApiResponse(code = 400, message = "Either Path/Body validation failed, or points exceeded Users total balance"),
		@ApiResponse(code = 500, message = "Unexpected errors have occurred"),
	})
	public List<ResponsePoint> deletePoints(
		@ApiParam("The User name") @PathVariable("name") @NotBlank String name,
		@Valid @RequestBody RequestPointDeduction points
	) {
		// Convert PointsEvents to PointsResponse removing cruft. Namely, epoch and uuid
		return this.userPointsService.deletePoints(name, points.getPoints())
			.stream()
			.map(pe -> new ResponsePoint(pe.getPayer(), pe.getPoints()))
			.collect(Collectors.toList());
	}
}
