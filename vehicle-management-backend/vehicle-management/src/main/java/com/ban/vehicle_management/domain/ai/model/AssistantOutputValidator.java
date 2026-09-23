package com.ban.vehicle_management.domain.ai.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Output validation pipeline for grounded answers: schema, citation allowlist,
 * hallucinated identifiers, forbidden claims and the action-success rule.
 */
public final class AssistantOutputValidator {

    public static final int MAX_RESPONSE_TEXT_LENGTH = 4000;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern CODE_PATTERN =
            Pattern.compile("\\b[A-Z]{2,6}-\\d{3,}\\b");
    private static final Pattern MONEY_PATTERN =
            Pattern.compile("\\b\\d[\\d.,]*\\s*(đ|vnd|₫)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern HOTLINE_PATTERN =
            Pattern.compile("\\b\\d{3,4}[.\\-\\s]?\\d{3,4}[.\\-\\s]?\\d{3,4}\\b");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    private static final List<Pattern> FORBIDDEN_SUCCESS_CLAIMS = List.of(
            Pattern.compile(".*\\b(đã\\s+tạo\\s+phiếu|đã\\s+thanh\\s+toán|đã\\s+hủy|đã\\s+cập\\s+nhật|đã\\s+gửi\\s+yêu\\s+cầu)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile(".*\\b(ticket\\s+created|payment\\s+successful|cancelled\\s+successfully)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));

    private static final Pattern SCRIPT_PATTERN =
            Pattern.compile("<\\s*script|javascript\\s*:|on\\w+\\s*=", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern SECRET_DISCLOSURE_PATTERN = Pattern.compile(
            "\\b(api[-_ ]?key|secret|access[-_ ]?token|refresh[-_ ]?token)\\b\\s*(là|:|=)\\s*\\S+",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    private AssistantOutputValidator() {
    }

    public record ValidationResult(boolean valid, List<String> violations) {
        public static ValidationResult ok() {
            return new ValidationResult(true, List.of());
        }
    }

    public record ValidationContext(
            Set<String> allowedCitationLabels,
            Set<UUID> allowedChunkIds,
            String userInput,
            List<String> evidenceTexts,
            List<String> toolOutputTexts,
            boolean toolSucceeded,
            boolean citationsRequired
    ) {
    }

    public static ValidationResult validate(AssistantResponseEnvelope envelope, ValidationContext context) {
        List<String> violations = new ArrayList<>();
        if (envelope == null) {
            violations.add("NULL_ENVELOPE");
            return new ValidationResult(false, violations);
        }
        String text = envelope.responseText() == null ? "" : envelope.responseText().strip();
        if (text.isEmpty()) {
            violations.add("EMPTY_RESPONSE_TEXT");
        }
        if (text.length() > MAX_RESPONSE_TEXT_LENGTH) {
            violations.add("RESPONSE_TEXT_TOO_LONG");
        }
        if (SCRIPT_PATTERN.matcher(text).find()) {
            violations.add("UNSAFE_HTML_SCRIPT");
        }
        if (SECRET_DISCLOSURE_PATTERN.matcher(text).find()) {
            violations.add("FORBIDDEN_SECRET_DISCLOSURE");
        }
        validateCitations(envelope, context, violations);
        validateIdentifiers(text, context, violations);
        validateSuccessClaims(text, context, violations);
        return new ValidationResult(violations.isEmpty(), List.copyOf(violations));
    }

    private static void validateCitations(
            AssistantResponseEnvelope envelope, ValidationContext context, List<String> violations) {
        List<AssistantResponseEnvelope.AssistantCitation> citations =
                envelope.citations() == null ? List.of() : envelope.citations();
        if (context.citationsRequired() && citations.isEmpty()) {
            violations.add("CITATION_REQUIRED");
        }
        Set<String> seen = new HashSet<>();
        for (AssistantResponseEnvelope.AssistantCitation citation : citations) {
            if (citation == null || citation.label() == null) {
                violations.add("INVALID_CITATION");
                continue;
            }
            if (!seen.add(citation.label())) {
                violations.add("DUPLICATE_CITATION:" + citation.label());
            }
            if (context.allowedCitationLabels() != null && !context.allowedCitationLabels().contains(citation.label())) {
                violations.add("CITATION_NOT_IN_ALLOWLIST:" + citation.label());
            }
            if (citation.chunkId() != null && context.allowedChunkIds() != null
                    && !context.allowedChunkIds().contains(citation.chunkId())) {
                violations.add("CITATION_CHUNK_NOT_IN_ALLOWLIST:" + citation.label());
            }
        }
    }

    private static void validateIdentifiers(String text, ValidationContext context, List<String> violations) {
        Set<String> known = knownIdentifiers(context);
        checkPattern(text, UUID_PATTERN, known, "HALLUCINATED_UUID", violations);
        checkPattern(text, CODE_PATTERN, known, "HALLUCINATED_CODE", violations);
        checkPattern(text, MONEY_PATTERN, known, "HALLUCINATED_AMOUNT", violations);
        checkPattern(text, HOTLINE_PATTERN, known, "HALLUCINATED_HOTLINE", violations);
        checkPattern(text, EMAIL_PATTERN, known, "HALLUCINATED_EMAIL", violations);
    }

    private static void checkPattern(
            String text, Pattern pattern, Set<String> known, String violationCode, List<String> violations) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = matcher.group();
            if (!known.contains(value) && !known.contains(value.toLowerCase(Locale.ROOT))) {
                violations.add(violationCode + ":" + value);
            }
        }
    }

    private static Set<String> knownIdentifiers(ValidationContext context) {
        Set<String> known = new HashSet<>();
        collect(known, context.userInput());
        if (context.evidenceTexts() != null) {
            for (String evidence : context.evidenceTexts()) {
                collect(known, evidence);
            }
        }
        if (context.toolOutputTexts() != null) {
            for (String output : context.toolOutputTexts()) {
                collect(known, output);
            }
        }
        return known;
    }

    private static void collect(Set<String> known, String text) {
        if (text == null) {
            return;
        }
        Matcher uuid = UUID_PATTERN.matcher(text);
        while (uuid.find()) {
            known.add(uuid.group());
        }
        Matcher code = CODE_PATTERN.matcher(text);
        while (code.find()) {
            known.add(code.group());
        }
        Matcher money = MONEY_PATTERN.matcher(text);
        while (money.find()) {
            known.add(money.group());
        }
        Matcher hotline = HOTLINE_PATTERN.matcher(text);
        while (hotline.find()) {
            known.add(hotline.group());
        }
        Matcher email = EMAIL_PATTERN.matcher(text);
        while (email.find()) {
            known.add(email.group());
        }
    }

    private static void validateSuccessClaims(String text, ValidationContext context, List<String> violations) {
        String lowered = text.toLowerCase(Locale.ROOT);
        for (Pattern pattern : FORBIDDEN_SUCCESS_CLAIMS) {
            if (pattern.matcher(lowered).matches() && !context.toolSucceeded()) {
                violations.add("UNVERIFIED_ACTION_SUCCESS_CLAIM");
                return;
            }
        }
    }
}
