package com.ban.vehicle_management.application.ai.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PiiRedactionService {

    private static final Pattern EMAIL = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?84|0)(?:\\d[ .-]?){8,10}\\d(?!\\d)");
    private static final Pattern TOKEN = Pattern.compile("(?i)\\b(?:bearer|token|password|refresh_token|access_token)\\s*[:=]\\s*\\S+");
    private static final Pattern ID_NUMBER = Pattern.compile("\\b\\d{9,12}\\b");
    private static final Pattern PAYMENT_CARD = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");
    private static final Pattern LICENSE_PLATE = Pattern.compile("\\b\\d{2}[A-Z][A-Z0-9]?[- ]?\\d{3,5}(?:\\.\\d{2})?\\b", Pattern.CASE_INSENSITIVE);

    public RedactionResult redact(String value) {
        if (value == null || value.isBlank()) {
            return new RedactionResult("", false, false);
        }
        String redacted = value;
        boolean sensitivePayment = PAYMENT_CARD.matcher(redacted).find();
        redacted = TOKEN.matcher(redacted).replaceAll("[SECRET_REDACTED]");
        redacted = EMAIL.matcher(redacted).replaceAll("[EMAIL_REDACTED]");
        redacted = PHONE.matcher(redacted).replaceAll("[PHONE_REDACTED]");
        redacted = PAYMENT_CARD.matcher(redacted).replaceAll("[PAYMENT_DATA_REDACTED]");
        redacted = ID_NUMBER.matcher(redacted).replaceAll("[ID_REDACTED]");
        redacted = LICENSE_PLATE.matcher(redacted).replaceAll("[LICENSE_PLATE_REDACTED]");
        return new RedactionResult(redacted, !redacted.equals(value), sensitivePayment);
    }

    public record RedactionResult(String value, boolean redacted, boolean sensitivePayment) {
    }
}
