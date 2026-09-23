package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VietnameseQueryNormalizerTest {

    @Test
    void normalizeShouldComposeCombiningCharacters() {
        // "é" as e + combining acute should compose to single codepoint form
        String decomposed = "quy tri\u0300nh đa\u0306ng ky\u0301";
        VietnameseQueryNormalizer.NormalizedQuery result = VietnameseQueryNormalizer.normalize(decomposed);
        assertEquals("quy trình đăng ký", result.normalized());
    }

    @Test
    void normalizeShouldKeepAccentedAndUnaccentedDistinct() {
        VietnameseQueryNormalizer.NormalizedQuery accented =
                VietnameseQueryNormalizer.normalize("Quy trình đăng ký thẻ");
        VietnameseQueryNormalizer.NormalizedQuery unaccented =
                VietnameseQueryNormalizer.normalize("quy trinh dang ky the");
        assertEquals("quy trình đăng ký thẻ", accented.normalized());
        assertEquals("quy trinh dang ky the", unaccented.normalized());
    }

    @Test
    void normalizeShouldCollapseWhitespaceAndLowercase() {
        VietnameseQueryNormalizer.NormalizedQuery result =
                VietnameseQueryNormalizer.normalize("  LÀM  THẺ\tXE\nMÁY  ");
        assertEquals("làm thẻ xe máy", result.normalized());
        assertEquals("LÀM  THẺ\tXE\nMÁY", result.original());
    }

    @Test
    void normalizeShouldPreserveIdentifiers() {
        VietnameseQueryNormalizer.NormalizedQuery uuid =
                VietnameseQueryNormalizer.normalize("Tra cứu 550e8400-e29b-41d4-a716-446655440000");
        assertEquals("tra cứu 550e8400-e29b-41d4-a716-446655440000", uuid.normalized());

        VietnameseQueryNormalizer.NormalizedQuery plate =
                VietnameseQueryNormalizer.normalize("Xe biển số 51F-123.45 ở đâu?");
        assertEquals("xe biển số 51f-123.45 ở đâu?", plate.normalized());
    }

    @Test
    void normalizeShouldRejectBlankAndOversized() {
        assertThrows(IllegalArgumentException.class, () -> VietnameseQueryNormalizer.normalize("   "));
        assertThrows(IllegalArgumentException.class, () -> VietnameseQueryNormalizer.normalize(null));
        assertThrows(IllegalArgumentException.class,
                () -> VietnameseQueryNormalizer.normalize("x".repeat(VietnameseQueryNormalizer.MAX_QUERY_LENGTH + 1)));
    }
}
