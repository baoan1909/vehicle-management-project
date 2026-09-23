package com.ban.vehicle_management.infrastructure.ai.file;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DocumentTypeDetectorTest {

    private final DocumentTypeDetector detector = new DocumentTypeDetector();

    @Test
    void isUtf8TextShouldValidateTheWholePayload() {
        byte[] payload = new byte[9_000];
        Arrays.fill(payload, (byte) 'a');
        payload[8_500] = (byte) 0xC3;
        payload[8_501] = (byte) 0x28;

        assertFalse(detector.isUtf8Text(payload));
    }

    @Test
    void isUtf8TextShouldAcceptVietnameseUtf8() {
        assertTrue(detector.isUtf8Text("Hướng dẫn đăng ký vé tháng".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void isUtf8TextShouldRejectNullControlBytes() {
        assertFalse(detector.isUtf8Text(new byte[]{'a', 0, 'b'}));
    }
}
