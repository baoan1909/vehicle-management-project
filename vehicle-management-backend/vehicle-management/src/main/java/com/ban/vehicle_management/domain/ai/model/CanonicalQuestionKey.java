package com.ban.vehicle_management.domain.ai.model;

/** Conservative matching view only. Never use this key for account/identifier lookups. */
public final class CanonicalQuestionKey {
    public static final String VERSION = "canonical-question-v1";
    private CanonicalQuestionKey() { }

    public static String of(String question) {
        String original = VietnameseQueryNormalizer.normalize(question).folded();
        // A generic troubleshooting follow-up must not collapse back into the initial instructions.
        if (VietnameseQueryNormalizer.isContextualFollowUp(question)) {
            return "FOLLOW_UP|" + original.replaceAll("[?.!]+$", "").strip();
        }
        String kind = original.matches("(?s).*\\b(?:tai sao|vi sao)\\b.*") ? "CAUSE"
                : original.matches("(?s).*\\b(?:bao lau|khi nao|luc nao)\\b.*") ? "TIME"
                : original.matches("(?s).*\\b(?:bao nhieu)\\b.*") ? "AMOUNT"
                : original.matches("(?s).*\\b(?:co nen)\\b.*") ? "ADVICE"
                : original.matches("(?s).*\\b(?:nghia la gi|la gi)\\b.*") ? "DEFINITION"
                : original.matches("(?s).*\\b(?:o dau)\\b.*") ? "LOCATION" : "GUIDANCE";
        String key = VietnameseQueryNormalizer.forLexicalSearch(question)
                .replaceAll("\\b(?:nhu )?the nao\\b", " ")
                .replaceAll("\\bdang ky coparking\\b", "dang ky tai khoan")
                .replaceAll("\\b(?:quen|dat lai|lay lai|khoi phuc) mat khau\\b", "khoi phuc mat khau")
                .replaceAll("\\bthu dien tu\\b", "email")
                .replaceAll("[?.!]+$", "").replaceAll("\\s+", " ").strip();
        return kind + "|" + key;
    }
}
