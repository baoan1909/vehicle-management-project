package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PublicContactRedactionTest {
    private final PiiRedactionService service = new PiiRedactionService();
    private final Set<String> approved = Set.of("0912345678", "support@coparking.vn");
    private final List<String> evidence = List.of("Hotline 0912345678, email support@coparking.vn.");

    @Test void preservesOnlyApprovedContactsPresentAsWholeValuesInCitedEvidence() {
        String text = "Hotline 0912345678, email support@coparking.vn, riêng private@example.com và 0987654321.";
        var actual = service.redactGroundedPublicContacts(text, approved, evidence);
        assertTrue(actual.value().contains("0912345678"));
        assertTrue(actual.value().contains("support@coparking.vn"));
        assertFalse(actual.value().contains("private@example.com"));
        assertFalse(actual.value().contains("0987654321"));
        assertTrue(actual.redacted());
    }
    @Test void failsClosedWithoutConfigurationOrCitedEvidence() {
        for (var result : List.of(service.redactGroundedPublicContacts("0912345678 support@coparking.vn", approved, List.of()),
                service.redactGroundedPublicContacts("0912345678 support@coparking.vn", Set.of(), evidence),
                service.redactGroundedPublicContacts("support@coparking.vn", approved, List.of("x-support@coparking.vn")))) {
            assertFalse(result.value().contains("0912345678"));
            assertFalse(result.value().contains("support@coparking.vn"));
        }
    }
    @Test void neverAllowsSecretsPaymentsSubstringsOrPersonalInputThroughException() {
        String text = "password=support@coparking.vn token=0912345678 card 4111091234567811 x-support@coparking.vn ID0912345678X";
        var result = service.redactGroundedPublicContacts(text, approved, evidence);
        assertFalse(result.value().contains("support@coparking.vn"));
        assertFalse(result.value().contains("0912345678"));
        assertFalse(result.value().contains("4111091234567811"));
        assertTrue(result.sensitivePayment());
        var input = service.redact("0912345678 support@coparking.vn");
        assertFalse(input.value().contains("0912345678"));
        assertFalse(input.value().contains("support@coparking.vn"));
    }
}
