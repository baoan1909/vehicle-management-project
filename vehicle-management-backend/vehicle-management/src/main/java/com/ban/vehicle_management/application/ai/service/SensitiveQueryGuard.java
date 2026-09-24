package com.ban.vehicle_management.application.ai.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Decides whether a redacted query is safe to cache. Anything that looks like
 * PII, business identifiers, secrets or write actions must bypass every cache.
 */
@Component
public class SensitiveQueryGuard {

    private static final Pattern EMAIL = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?84|0)(?:\\d[ .-]?){8,10}\\d(?!\\d)");
    private static final Pattern TOKEN = Pattern.compile("(?i)\\b(bearer|token|password|refresh_token|access_token|api[_-]?key)\\b");
    private static final Pattern LICENSE_PLATE = Pattern.compile("\\b\\d{2}[A-Z][A-Z0-9]?[- ]?\\d{3,5}(?:\\.\\d{2})?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TICKET_CODE = Pattern.compile("\\b[A-Z]{2,5}-\\d{4,}\\b");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");
    private static final Pattern REDACTED_MARKER = Pattern.compile("\\[(EMAIL|PHONE|SECRET|PAYMENT_DATA|ID|LICENSE_PLATE)_REDACTED\\]");

    public boolean isCacheable(String redactedQuery) {
        if (redactedQuery == null || redactedQuery.isBlank()) {
            return false;
        }
        if (REDACTED_MARKER.matcher(redactedQuery).find()) {
            return false;
        }
        return !EMAIL.matcher(redactedQuery).find()
                && !PHONE.matcher(redactedQuery).find()
                && !TOKEN.matcher(redactedQuery).find()
                && !LICENSE_PLATE.matcher(redactedQuery).find()
                && !TICKET_CODE.matcher(redactedQuery).find()
                && !UUID_PATTERN.matcher(redactedQuery).find();
    }
}
