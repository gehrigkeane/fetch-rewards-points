package com.gehrig.fetch.points.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom {@link ZonedDateTime} deserializer with moderate flexibility.
 */
public class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZonedDateTimeDeserializer.class);
	private static final String ZDT_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSz";
	private static final DateTimeFormatter ZDT_FORMAT = DateTimeFormatter.ofPattern(ZDT_FORMAT_PATTERN);

	@Override
	public ZonedDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
		throws IOException {

		final String dateText = jsonParser.getText();

		try {
			return ZonedDateTime.parse(dateText, ZDT_FORMAT);
		} catch (DateTimeParseException e) {
			LOGGER.debug("Failed to date `{}` with pattern {}", dateText, ZDT_FORMAT_PATTERN);
		}

		try {
			return ZonedDateTime.parse(dateText, DateTimeFormatter.ISO_ZONED_DATE_TIME);
		} catch (DateTimeParseException e) {
			LOGGER.debug("Failed to date `{}` with pattern {}", dateText, "ISO_ZONED_DATE_TIME");
		}

		/*
			Let's try some LocalDateTimes!
 		*/

		try {
			return ZonedDateTime.of(LocalDateTime.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE_TIME), ZoneOffset.UTC);
		} catch (DateTimeParseException e) {
			LOGGER.debug("Failed to date `{}` with pattern {}", dateText, "ISO_LOCAL_DATE_TIME");
		}

		try {
			return ZonedDateTime.of(
				LocalDate.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(),
				ZoneOffset.UTC
			);
		} catch (DateTimeParseException e) {
			LOGGER.debug("Failed to date `{}` with pattern {}", dateText, "ISO_LOCAL_DATE");
			throw e;
		}
	}
}
