package com.acommon.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        value = value.trim();
        // Format YYYY-MM-DD
        if (value.length() == 10) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        }
        // Format ISO UTC Z (ex: 2026-09-04T12:00:00.000Z)
        if (value.endsWith("Z")) {
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        // Format avec espace (ex: 2026-09-04 12:00:00)
        if (value.contains(" ")) {
            value = value.replace(" ", "T");
        }
        return LocalDateTime.parse(value);
    }
}
