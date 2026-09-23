package com.ban.vehicle_management.domain.ai.model;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deterministic-first intent router. Explicit rules win; anything ambiguous
 * falls back to STATIC_KNOWLEDGE when question-like, otherwise OUT_OF_SCOPE.
 * A model classifier may only be consulted for ambiguous cases and must return
 * a structured enum validated here (fail-safe fallback included).
 */
public final class AssistantIntentRouter {

    private static final List<Pattern> GREETING = List.of(
            Pattern.compile("^(xin\\s+chào|chào(\\s+bạn)?|hello|hi|hey|chào\\s+buổi\\s+(sáng|chiều|tối))\\b[\\s!.,]*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("^(xin chào|chào).*\\bcoparking\\b.*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));

    private static final List<Pattern> HANDOFF = List.of(
            Pattern.compile(".*\\b(gặp|nói chuyện|kết nối).{0,20}\\b(nhân viên|người thật|tổng đài|hỗ trợ viên)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile(".*\\bchuyển.{0,10}\\b(nhân viên|người)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));

    private static final List<Pattern> SECURITY_REFUSAL = List.of(
            Pattern.compile(".*\\b(api[-_ ]?key|secret|token|cvv|cvc)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile(".*\\b(tiết lộ|cho xem|hiển thị|đọc|gửi).{0,30}\\b(mật khẩu|password|api[-_ ]?key|secret|token)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile(".*\\b(thẻ\\s+tín\\s+dụng|card\\s+number|cvv|cvc)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile(".*\\b(system\\s+prompt|bỏ\\s+qua\\s+(hướng\\s+dẫn|chỉ\\s+dẫn)|ignore\\s+(previous|all)\\s+instructions|reveal\\s+.*prompt)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));

    private static final List<Pattern> WRITE_ACTION = List.of(
            Pattern.compile(".*\\b(tạo|mở|lập)\\s+(phiếu|ticket|yêu cầu)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));

    private static final List<Pattern> PERSONAL_DATA = List.of(
            Pattern.compile(".*\\b(của\\s+tôi|my\\b).{0,30}\\b(phiếu|vé|đăng ký|ticket|subscription|xe)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile(".*\\b(phiếu|vé|đăng ký)\\b.{0,20}\\b(tôi|mình)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile(".*\\b[A-Z]{2,5}-\\d{4,}\\b.*"));

    private static final List<Pattern> OUT_OF_SCOPE = List.of(
            Pattern.compile(".*\\b(chứng khoán|forex|crypto|bitcoin|bóng đá|dự đoán\\s+xổ\\s+số)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));

    private static final List<Pattern> STATIC_KNOWLEDGE = List.of(
            Pattern.compile(".*\\b(hướng dẫn|quy trình|thủ tục|cách|quên mật khẩu|đăng nhập google)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile(".*\\b(vé tháng|bảng giá|phí gửi xe|thanh toán|đóng tiền|hủy vé|huỷ vé|vnpay|hoàn tiền|xe vào|xe ra|thẻ xe|mất thẻ|giờ hoạt động|địa điểm|sla)\\b.*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));

    private AssistantIntentRouter() {
    }

    public record IntentDecision(AssistantIntent intent, String reason) {
    }

    public static IntentDecision route(String rawInput) {
        String input = rawInput == null ? "" : rawInput.strip();
        if (input.isEmpty()) {
            return new IntentDecision(AssistantIntent.OUT_OF_SCOPE, "empty-input");
        }
        String lowered = input.toLowerCase(Locale.ROOT);
        if (matchesAny(GREETING, lowered)) {
            return new IntentDecision(AssistantIntent.GREETING, "deterministic-greeting");
        }
        if (matchesAny(SECURITY_REFUSAL, lowered)) {
            return new IntentDecision(AssistantIntent.SECURITY_REFUSAL, "deterministic-sensitive");
        }
        if (matchesAny(HANDOFF, lowered)) {
            return new IntentDecision(AssistantIntent.HANDOFF_REQUEST, "deterministic-handoff");
        }
        if (matchesAny(WRITE_ACTION, lowered)) {
            return new IntentDecision(AssistantIntent.WRITE_ACTION, "deterministic-write");
        }
        if (matchesAny(PERSONAL_DATA, lowered)) {
            return new IntentDecision(AssistantIntent.PERSONAL_DATA, "deterministic-personal");
        }
        if (matchesAny(OUT_OF_SCOPE, lowered)) {
            return new IntentDecision(AssistantIntent.OUT_OF_SCOPE, "deterministic-out-of-scope");
        }
        if (matchesAny(STATIC_KNOWLEDGE, lowered)) {
            return new IntentDecision(AssistantIntent.STATIC_KNOWLEDGE, "deterministic-domain-knowledge");
        }
        if (isQuestionLike(lowered)) {
            return new IntentDecision(AssistantIntent.STATIC_KNOWLEDGE, "fallback-question-like");
        }
        return new IntentDecision(AssistantIntent.OUT_OF_SCOPE, "fallback-out-of-scope");
    }

    /** Validates a model-classified enum value; unknown values fail safe to OUT_OF_SCOPE. */
    public static IntentDecision fromModelClassifier(String modelValue) {
        if (modelValue == null) {
            return new IntentDecision(AssistantIntent.OUT_OF_SCOPE, "model-null");
        }
        try {
            AssistantIntent intent = AssistantIntent.valueOf(modelValue.strip().toUpperCase(Locale.ROOT));
            return new IntentDecision(intent, "model-classified");
        } catch (IllegalArgumentException exception) {
            return new IntentDecision(AssistantIntent.OUT_OF_SCOPE, "model-unknown");
        }
    }

    private static boolean matchesAny(List<Pattern> patterns, String lowered) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(lowered).matches()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isQuestionLike(String lowered) {
        return lowered.contains("?")
                || lowered.startsWith("làm sao") || lowered.startsWith("làm thế nào")
                || lowered.startsWith("cách ") || lowered.startsWith("quy trình")
                || lowered.startsWith("thủ tục") || lowered.startsWith("giá ")
                || lowered.startsWith("phí ") || lowered.startsWith("ở đâu")
                || lowered.startsWith("khi nào") || lowered.startsWith("how ")
                || lowered.startsWith("what ") || lowered.startsWith("where ");
    }
}
