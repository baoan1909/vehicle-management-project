package com.ban.vehicle_management.domain.ai.model;

import java.text.Normalizer;
import java.util.Locale;

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

    /** Maximum normalized query length accepted for retrieval. */
    public static final int MAX_QUERY_LENGTH = 500;

    private VietnameseQueryNormalizer() {
    }

    public record NormalizedQuery(String original, String normalized) {
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
