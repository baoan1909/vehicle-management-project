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
    private static final Pattern LICENSE_PLATE = Pattern.compile(
            "(?<![A-Z0-9])(?:80[- .]?\\d{3}[- .]?(?:NG|QT|CV|NN)[- .]?\\d{2,3}"
                    + "|\\d{2}[A-Z](?:[A-Z]|\\d)?[- .]?(?:\\d{4}|\\d{3}(?:[ .]?\\d{2})))(?![A-Z0-9])",
            Pattern.CASE_INSENSITIVE
    );

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

    /** Public contact values remain visible only in grounded output with cited evidence.
     * Input/tool redaction continues to use the strict redact method above. */
    public RedactionResult redactGroundedPublicContacts(String value, java.util.Set<String> approved,
            java.util.List<String> citedEvidence) {
        if (value == null || value.isBlank() || approved == null || approved.isEmpty()
                || citedEvidence == null || citedEvidence.isEmpty()) return redact(value);
        java.util.Set<String> allowed = new java.util.HashSet<>();
        for (String item : approved) {
            if (item != null && (EMAIL.matcher(item).matches() || PHONE.matcher(item).matches()))
                allowed.add(item.toLowerCase(java.util.Locale.ROOT));
        }
        java.util.Set<String> evidenced = new java.util.HashSet<>();
        for (String source : citedEvidence) {
            if (source == null) continue;
            for (Pattern pattern : java.util.List.of(EMAIL, PHONE)) {
                var matcher = pattern.matcher(source);
                while (matcher.find()) {
                    if (standaloneContact(source, matcher.start(), matcher.end()))
                        evidenced.add(matcher.group().toLowerCase(java.util.Locale.ROOT));
                }
            }
        }
        allowed.retainAll(evidenced);
        boolean payment = PAYMENT_CARD.matcher(value).find();
        // Secrets and payment data are never eligible for the contact exception.
        String prepared = TOKEN.matcher(value).replaceAll("[SECRET_REDACTED]");
        prepared = PAYMENT_CARD.matcher(prepared).replaceAll("[PAYMENT_DATA_REDACTED]");
        java.util.Map<String, String> protectedValues = new java.util.LinkedHashMap<>();
        for (Pattern pattern : java.util.List.of(EMAIL, PHONE)) {
            var matcher = pattern.matcher(prepared);
            StringBuffer output = new StringBuffer();
            while (matcher.find()) {
                String candidate = matcher.group();
                if (!allowed.contains(candidate.toLowerCase(java.util.Locale.ROOT))
                        || !standaloneContact(prepared, matcher.start(), matcher.end())) continue;
                String placeholder;
                do {
                    placeholder = "PUBLICCONTACT" + java.util.UUID.randomUUID().toString()
                            .replace("-", "").replace('0', 'g').replace('1', 'h').replace('2', 'i')
                            .replace('3', 'j').replace('4', 'k').replace('5', 'l').replace('6', 'm')
                            .replace('7', 'n').replace('8', 'o').replace('9', 'p');
                } while (value.contains(placeholder) || protectedValues.containsKey(placeholder));
                protectedValues.put(placeholder, candidate);
                matcher.appendReplacement(output, java.util.regex.Matcher.quoteReplacement(placeholder));
            }
            matcher.appendTail(output);
            prepared = output.toString();
        }
        String result = redact(prepared).value();
        for (var entry : protectedValues.entrySet()) result = result.replace(entry.getKey(), entry.getValue());
        return new RedactionResult(result, !result.equals(value), payment);
    }

    private static boolean standaloneContact(String value, int start, int end) {
        return (start == 0 || !identifierCharacter(value.charAt(start - 1)))
                && (end == value.length() || !identifierCharacter(value.charAt(end)));
    }

    private static boolean identifierCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '-';
    }

    public record RedactionResult(String value, boolean redacted, boolean sensitivePayment) {
    }
}
