package com.gehrig.fetch.points.dto;

import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
public class ResponsePoint {

	@NotNull(message = "payer must not be null")
	String payer;

	@NotNull(message = "points must not be null")
	Long points;
}
