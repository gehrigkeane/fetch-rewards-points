package com.gehrig.fetch.points.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestPointDeduction {

	@NotNull(message = "points must not be null")
	@Min(value = 1, message = "points must be positive")
	Long points;
}
