package com.resilio.demo.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.regex.Pattern;

@Service
public class DataProcessingService {

    private static final Pattern VALID_PAYLOAD_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{4,32}$");

    public record ProcessingResult(boolean valid, String message, String processedValue, String timestamp) {}

    /**
     * Business logic for validating and processing payload inputs.
     * Required for Selenium functional test verification.
     */
    public ProcessingResult processPayload(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ProcessingResult(false, "Validation Error: Input payload cannot be empty.", null, Instant.now().toString());
        }

        String trimmed = input.trim();
        if (!VALID_PAYLOAD_PATTERN.matcher(trimmed).matches()) {
            return new ProcessingResult(false, "Validation Error: Input must be 4 to 32 alphanumeric characters, dashes, or underscores.", null, Instant.now().toString());
        }

        String transformed = "PROCESSED::" + trimmed.toUpperCase();
        return new ProcessingResult(true, "Validation Passed: Successfully verified and registered payload.", transformed, Instant.now().toString());
    }
}
