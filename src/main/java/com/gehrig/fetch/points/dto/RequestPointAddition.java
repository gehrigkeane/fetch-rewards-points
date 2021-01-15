package com.gehrig.fetch.points.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gehrig.fetch.points.configuration.ZonedDateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestPointAddition {

	@NotBlank(message = "payer must not be null or blank")
	@Size(min = 1, max = 63, message = "payer must be between {min} and {max} characters long")
	String payer;

	// Heck, why not support ~2^63 points!
	@NotNull(message = "points must not be null")
	Long points;

	@JsonDeserialize(using = ZonedDateTimeDeserializer.class)
	ZonedDateTime date;
}
