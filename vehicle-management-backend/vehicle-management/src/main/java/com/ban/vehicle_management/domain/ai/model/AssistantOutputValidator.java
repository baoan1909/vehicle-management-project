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

    public static final String VERSION = "output-validation-v4";

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
            Pattern.compile("\\bđã\\s+(?:được\\s+)?(?:hoàn(?:\\s+lại)?\\s+tiền|chuyển\\s+tiền|trừ\\s+(?:tiền|ví)|(?:đặt|giữ|xác\\s+nhận(?:\\s+giữ)?)\\s+chỗ|áp\\s+dụng\\s+(?:mã|voucher)|thanh\\s+toán)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("\\b(?:tiền|số\\s+dư)\\s+đã\\s+được\\s+(?:hoàn|chuyển|trừ|cộng)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("\\b(?:đặt\\s+chỗ|chỗ|hóa\\s+đơn|phiếu\\s+hỗ\\s+trợ)\\s+đã\\s+được\\s+(?:tạo|giữ|hủy|xác\\s+nhận)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("\\b(?:refunded|refund\\s+successful|reservation\\s+(?:confirmed|created)|voucher\\s+applied)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("\\b(?:phiếu|vé|thông\\s+tin)\\s+đã\\s+được\\s+(?:tạo|hủy|cập\\s+nhật)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("\\b(đã\\s+(?:được\\s+)?tạo\\s+phiếu|đã\\s+thanh\\s+toán|đã\\s+(?:được\\s+)?hủy|đã\\s+(?:được\\s+)?cập\\s+nhật|đã\\s+gửi\\s+yêu\\s+cầu)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("\\b(ticket\\s+created|payment\\s+successful|cancelled\\s+successfully)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));

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
        if (AssistantResponseStyle.containsUnnecessaryInternalDetail(text, context.userInput())) {
            violations.add("UNNECESSARY_INTERNAL_DETAIL");
        }
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

    private static final Pattern CLAUSE_BOUNDARY = Pattern.compile("[.!?;,:\\n]");
    private static final Pattern CONDITIONAL_PREFIX = Pattern.compile(
            "\\b(?:neu|sau khi|truoc khi|khi|voi|doi voi|trong truong hop|if|after|before)(?:\\s+(?:ban|khach(?: hang)?|nhan vien|hoa don|phieu(?: ho tro)?|ve(?: thang)?|tai khoan|he thong|dat cho|don dat cho|ma(?: uu dai| giam gia)?|khoan (?:thanh toan|hoan tien)|it|the ticket|the payment))?\\s*$");
    private static final Pattern VERIFICATION_PREFIX = Pattern.compile("\\b(?:kiem tra|xac minh|hoi xem|check whether|verify whether)\\b");
    private static final Pattern ASSISTANT_ACTOR = Pattern.compile("\\b(?:toi|minh|tro ly|he thong|i|we|assistant|system)\\b");
    private static final Pattern NEGATED_CLAIM_PREFIX = Pattern.compile(
            "\\b(?:chua|khong)(?: the| nen| duoc| du(?: de)?)? (?:xac nhan|khang dinh|bao|thong bao|chung minh|ket luan|co bang chung|(?:bao dam|dam bao)(?: co)?|co nghia(?: la)?|dong nghia(?: la)?)(?: rang)?(?:\\s+(?:ban|hoa don|dat cho|ve|ma|khoan thanh toan))?\\s*$");
    private static final Pattern NON_PROOF_PREFIX = Pattern.compile(
            "\\b(?:khong phai(?: la)?|khong co(?: du)?|chua(?: co)? du) (?:bang chung|can cu|co so)(?: doc lap| duy nhat| day du| chac chan| hop le)?(?: de (?:xac nhan|ket luan|chung minh))?(?: rang)?(?:\\s+(?:ban|hoa don|dat cho|ve|ma|khoan thanh toan))?\\s*$");
    private static final Pattern NEGATED_INFERENCE_PREFIX = Pattern.compile(
            "\\b(?:ban )?khong(?: nen)?(?: chi)? dua [^.!?;,:\\n]{1,240} de (?:ket luan|xac nhan)(?:\\s+(?:ban|hoa don|dat cho|ve|ma|khoan thanh toan))?\\s*$");
    private static final Pattern QUESTION_END = Pattern.compile(".*\\b(?:chua|hay chua|khong|hay khong)\\s*$");
    private static final Pattern NEGATED_ENTITY_PREFIX = Pattern.compile("\\b(?:khong|chua) phai(?: la)?(?: mot)?\\s*$");
    private static final Pattern EVIDENCE_SUBJECT_PREFIX = Pattern.compile("^(?:(?:luu y(?: rang)?|can luu y(?: rang)?|tuy nhien|vi vay)\\s+)?(?:viec|thong bao|dong chu)\\s*$");
    private static final Pattern NON_PROOF_SUFFIX = Pattern.compile("^(?:khong|chua)(?: tu)? (?:chung minh|xac nhan|la bang chung|co nghia|dong nghia)\\b");

    private static void validateSuccessClaims(String text, ValidationContext context, List<String> violations) {
        if (context.toolSucceeded()) return;
        for (Pattern pattern : FORBIDDEN_SUCCESS_CLAIMS) {
            Matcher claim = pattern.matcher(text);
            while (claim.find()) {
                if (!isConditionalOrVerification(text, claim.start(), claim.end())) {
                    violations.add("UNVERIFIED_ACTION_SUCCESS_CLAIM");
                    return;
                }
            }
        }
    }

    private static boolean isConditionalOrVerification(String text, int start, int end) {
        int clauseStart = 0;
        int clauseEnd = text.length();
        Matcher boundaries = CLAUSE_BOUNDARY.matcher(text);
        while (boundaries.find()) {
            if (boundaries.start() < start) clauseStart = boundaries.end();
            else if (boundaries.start() >= end) { clauseEnd = boundaries.start(); break; }
        }
        String prefix = VietnameseQueryNormalizer.fold(text.substring(clauseStart, start)).strip();
        String suffix = VietnameseQueryNormalizer.fold(text.substring(end, clauseEnd)).strip();
        if (CONDITIONAL_PREFIX.matcher(prefix).find() || NEGATED_CLAIM_PREFIX.matcher(prefix).find()
                || NON_PROOF_PREFIX.matcher(prefix).find() || NEGATED_INFERENCE_PREFIX.matcher(prefix).find()
                || NEGATED_ENTITY_PREFIX.matcher(prefix).find()) return true;
        if (EVIDENCE_SUBJECT_PREFIX.matcher(prefix).find() && NON_PROOF_SUFFIX.matcher(suffix).find()) return true;
        boolean assistantActor = ASSISTANT_ACTOR.matcher(prefix.replaceAll("\\bcua (?:toi|minh)\\b", "")).find();
        if (!assistantActor && clauseEnd < text.length() && text.charAt(clauseEnd) == '?'
                && QUESTION_END.matcher(suffix).matches()) return true;
        // A verification instruction is not a completed action. An explicit assistant/system
        // actor remains blocked, including a new claim after a comma or line break.
        return VERIFICATION_PREFIX.matcher(prefix).find() && !assistantActor;
    }
}
