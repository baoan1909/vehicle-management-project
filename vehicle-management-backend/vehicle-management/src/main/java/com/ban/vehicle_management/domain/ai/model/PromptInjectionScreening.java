package com.ban.vehicle_management.domain.ai.model;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Multi-pattern prompt-injection screening for retrieved evidence. This is one
 * layer only: ingestion-time scan, runtime context isolation, system
 * instruction and output validation form the remaining layers.
 */
public final class PromptInjectionScreening {

    private static final List<Pattern> HIGH_RISK = List.of(
            Pattern.compile("ignore\\s+(?:all\\s+)?(?:previous\\s+|prior\\s+)?instructions", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("bỏ\\s+qua\\s+(mọi|tất cả|các)?\\s*(hướng dẫn|chỉ dẫn|chỉ thị)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("reveal\\s+(your\\s+|the\\s+)?system\\s+prompt|tiết\\s+lộ\\s+.*system\\s+prompt", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("exfiltrat|gửi\\s+.*\\b(secret|token|api[-_ ]?key|mật khẩu)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("<\\s*script|javascript\\s*:|on\\w+\\s*=", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));

    private static final List<Pattern> MEDIUM_RISK = List.of(
            Pattern.compile("\\b(hãy\\s+|vui lòng\\s+)?(gọi|thực hiện|kích hoạt)\\s+(tool|hành động|hàm)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("\\bact\\s+as\\s+(admin|root|system)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS),
            Pattern.compile("đóng\\s+vai\\s+(admin|quản trị|hệ thống)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));

    private PromptInjectionScreening() {
    }

    public record ScreeningResult(boolean suspicious, boolean highRisk, int matchedPatterns, double score) {
    }

    public static ScreeningResult scan(String content) {
        if (content == null || content.isBlank()) {
            return new ScreeningResult(false, false, 0, 0.0);
        }
        int high = countMatches(HIGH_RISK, content);
        int medium = countMatches(MEDIUM_RISK, content);
        double score = Math.min(1.0, high * 0.6 + medium * 0.2);
        return new ScreeningResult(score >= 0.2, high > 0, high + medium, score);
    }

    private static int countMatches(List<Pattern> patterns, String content) {
        int count = 0;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(content).find()) {
                count++;
            }
        }
        return count;
    }
}
