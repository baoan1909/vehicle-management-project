package com.ban.vehicle_management.domain.ai.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Unicode normalization for Vietnamese retrieval queries.
 *
 * <p>Produces a stable normalized form for matching while keeping the original
 * (redacted) query for audit. Normalization never alters the set of identifier
 * characters: UUIDs, ticket/invoice codes, digits and license-plate separators
 * survive unchanged apart from case folding, which the lexical index also
 * applies via {@code plainto_tsquery('simple', ...)}.</p>
 */
public final class VietnameseQueryNormalizer {
    public static final String LEXICAL_VERSION = "vi-lexical-v5";

    /** Maximum normalized query length accepted for retrieval. */
    public static final int MAX_QUERY_LENGTH = 500;
    private static final Pattern CONTEXTUAL_FOLLOW_UP = Pattern.compile(
            "^huong dan ([^.!?]+)\\. (?:van (?:bi )?loi|khong duoc|(?:toi )?lam roi(?: nhung)? (?:van )?chua duoc|buoc tiep theo|cai do o dau|can lam gi|phai lam sao)[?.!]*$");

    private VietnameseQueryNormalizer() {
    }

    public record NormalizedQuery(String original, String normalized) {
        /** Matching view only: never use this value to look up identifiers or render answers. */
        public String folded() {
            return fold(normalized);
        }
    }

    /** Lexical-only framing removal; the original query still drives vector retrieval and audit. */
    public static String forLexicalSearch(String query) {
        String folded = normalize(query).folded();
        var followUp = CONTEXTUAL_FOLLOW_UP.matcher(folded);
        if (followUp.matches()) return followUp.group(1).strip();
        String terms = folded.replaceAll("\\bcan kiem tra gi(?=\\s|[?.!]|$)", "kiem tra")
                .replaceAll("\\bthi sao(?=\\s|[?.!]|$)", " ").replaceFirst("^(?:toi|minh|em)\\s+(?:muon|can)\\s+(?:biet|bit)(?:\\s+cach)?\\s+", "")
                .replaceAll("\\b(?:thi\\s+)?(?:toi muon biet cach|toi muon biet|toi can biet|cho toi biet|cho biet|bao nhieu|phai lam sao|phai lam gi|lam the nao|nhu the nao|can lam gi|huong dan|tai sao|vi sao|lam sao(?: biet)?|luc nao|nhu nao|nghia la gi|co nen|o dau|la gi|cach)\\b", " ")
                .replaceAll("(?<!\\S)(?:(?<!hien )thi(?!\\s+(?:truong|xa|tran)\\b)|va(?!\\s+(?:cham|quet)\\b)|nhung)(?!\\S)", " ")
                .replaceAll("(?<!\\S)chac(?: chan)?(?=\\s|[?.!]|$)", "bao dam")
                .replaceAll("\\bdk\\b", "dang ky")
                .replaceAll("\\b(?:ko|khongg)\\b", "khong")
                .replaceAll("\\bdc\\b", "duoc")
                .replaceAll("\\s+", " ").replaceAll("\\s+([?.!])", "$1").strip();
        return terms.isBlank() || terms.matches("[?.! ]*") ? folded : terms;
    }

    public static boolean isContextualFollowUp(String query) {
        return query != null && CONTEXTUAL_FOLLOW_UP.matcher(normalize(query).folded()).matches();
    }

    public static String fold(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes a raw user query: NFC composition, lower-case (ROOT), whitespace
     * collapsing and removal of ISO control characters (except whitespace).
     *
     * @throws IllegalArgumentException when the query is blank or exceeds {@link #MAX_QUERY_LENGTH}
     */
    public static NormalizedQuery normalize(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        String original = rawQuery.strip();
        if (original.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("query must not exceed " + MAX_QUERY_LENGTH + " characters");
        }
        String composed = Normalizer.normalize(original, Normalizer.Form.NFC);
        StringBuilder builder = new StringBuilder(composed.length());
        boolean pendingSpace = false;
        for (int index = 0; index < composed.length(); index++) {
            char character = composed.charAt(index);
            if (Character.isWhitespace(character)) {
                pendingSpace = true;
                continue;
            }
            if (Character.isISOControl(character)) {
                continue;
            }
            if (pendingSpace && !builder.isEmpty()) {
                builder.append(' ');
            }
            pendingSpace = false;
            builder.append(character);
        }
        String collapsed = builder.toString();
        if (collapsed.isEmpty()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        return new NormalizedQuery(original, collapsed.toLowerCase(Locale.ROOT));
    }
}
