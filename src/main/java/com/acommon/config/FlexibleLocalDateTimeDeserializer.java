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
        // Format date 10 caractères : YYYY-MM-DD, DD/MM/YYYY ou DD-MM-YYYY
        if (value.length() == 10) {
            if (value.contains("/")) {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy")).atStartOfDay();
            }
            if (value.charAt(2) == '-' && value.charAt(5) == '-') {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay();
            }
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        }
        // Format avec slash et heure : DD/MM/YYYY HH:mm ou DD/MM/YYYY HH:mm:ss
        if (value.contains("/")) {
            try {
                if (value.length() == 16) {
                    return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                }
                if (value.length() >= 19) {
                    return LocalDateTime.parse(value.substring(0, 19), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                }
            } catch (Exception ignored) {}
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
