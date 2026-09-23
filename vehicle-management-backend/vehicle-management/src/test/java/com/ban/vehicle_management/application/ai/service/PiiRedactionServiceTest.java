package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PiiRedactionServiceTest {

    private final PiiRedactionService service = new PiiRedactionService();

    @Test
    void shouldRedactEmailPhoneAndLicensePlate() {
        PiiRedactionService.RedactionResult result = service.redact(
                "Email a@example.com, phone 0912345678, plate 51A-12345"
        );

        assertTrue(result.redacted());
        assertTrue(result.value().contains("[EMAIL_REDACTED]"));
        assertTrue(result.value().contains("[PHONE_REDACTED]"));
        assertTrue(result.value().contains("[LICENSE_PLATE_REDACTED]"));
        assertFalse(result.sensitivePayment());
    }

    @Test
    void shouldDetectPaymentData() {
        PiiRedactionService.RedactionResult result = service.redact("Card 4111 1111 1111 1111");

        assertTrue(result.sensitivePayment());
        assertTrue(result.value().contains("[PAYMENT_DATA_REDACTED]"));
    }
}
