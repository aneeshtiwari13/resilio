package com.resilio.platform.observability;

import java.util.regex.Pattern;

/**
 * Utility for masking secrets, tokens, passwords, and sensitive headers in log messages and payloads.
 */
public final class SecretMasker {

    private static final String MASK = "[REDACTED]";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(?i)(\"?(?:password|token|secret|api[_-]?key|client[_-]?secret)\"?\\s*[:=]\\s*\"?)([^\"\\s,}]+)(\"?)");

    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)(bearer\\s+)[a-zA-Z0-9_\\-\\.]+", Pattern.CASE_INSENSITIVE);

    private static final Pattern BASIC_AUTH_PATTERN = Pattern.compile(
            "(?i)(basic\\s+)[a-zA-Z0-9+/=]+", Pattern.CASE_INSENSITIVE);

    private SecretMasker() {
    }

    /**
     * Replaces sensitive field values in strings (e.g. JSON payloads, connection strings, query strings).
     *
     * @param input Raw text
     * @return Sanitized text with sensitive credentials redacted
     */
    public static String mask(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = PASSWORD_PATTERN.matcher(input).replaceAll("$1" + MASK + "$3");
        result = BEARER_PATTERN.matcher(result).replaceAll("$1" + MASK);
        result = BASIC_AUTH_PATTERN.matcher(result).replaceAll("$1" + MASK);
        return result;
    }
}
