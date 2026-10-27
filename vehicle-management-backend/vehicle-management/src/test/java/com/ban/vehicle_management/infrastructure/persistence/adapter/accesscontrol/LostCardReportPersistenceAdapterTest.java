package com.ban.vehicle_management.infrastructure.persistence.adapter.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LostCardReportPersistenceAdapterTest {

    @Test
    void toKeywordPatternUsesEmptyStringWhenKeywordIsMissing() {
        assertEquals("", LostCardReportPersistenceAdapter.toKeywordPattern(null));
        assertEquals("", LostCardReportPersistenceAdapter.toKeywordPattern("   "));
    }

    @Test
    void toKeywordPatternNormalizesNonBlankKeywordToLowercasePattern() {
        assertEquals(
                "%54-l1.9999%",
                LostCardReportPersistenceAdapter.toKeywordPattern(" 54-L1.9999 ")
        );
    }
}
