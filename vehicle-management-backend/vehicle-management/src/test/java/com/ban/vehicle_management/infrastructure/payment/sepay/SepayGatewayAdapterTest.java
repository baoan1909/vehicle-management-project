package com.ban.vehicle_management.infrastructure.payment.sepay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.billing.payment.model.command.SepayWebhookCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SepayGatewayAdapterTest {
    private static final String SECRET = "test-webhook-secret";

    private SepayGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        SepayProperties properties = new SepayProperties();
        properties.setEnabled(true);
        properties.setBankCode("MBBank");
        properties.setAccountNumber("0123456789");
        properties.setAccountHolder("CO PARKING TEST");
        properties.setWebhookSecret(SECRET);
        properties.setWebhookTimestampTolerance(Duration.ofMinutes(5));
        adapter = new SepayGatewayAdapter(properties, new ObjectMapper());
    }

    @Test
    void createsVietQrInstructionWithExactAmountAndReference() {
        var instruction = adapter.createPaymentInstruction(
                "CPR123", new BigDecimal("8000"), Instant.parse("2026-11-13T02:00:00Z"));

        assertTrue(instruction.qrCodeUrl().contains("amount=8000"));
        assertTrue(instruction.qrCodeUrl().contains("des=CPR123"));
        assertEquals("CPR123", instruction.transferContent());
    }

    @Test
    void verifiesOfficialHmacHeadersAndPayload() throws Exception {
        byte[] body = ("{\"id\":12345,\"gateway\":\"MBBank\","
                + "\"transactionDate\":\"2026-11-13 09:30:00\","
                + "\"accountNumber\":\"0123456789\",\"subAccount\":null,"
                + "\"code\":\"CPR123\",\"content\":\"CPR123 thanh toan\","
                + "\"transferType\":\"in\",\"transferAmount\":8000,"
                + "\"referenceCode\":\"FT123\"}").getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String signature = sign(timestamp, body);

        var result = adapter.verifyWebhook(new SepayWebhookCommand(body, signature, timestamp));

        assertTrue(result.authenticated());
        assertEquals("CPR123", result.transactionRef());
        assertEquals(new BigDecimal("8000"), result.amount());
        assertEquals("FT123", result.providerTransactionNo());
    }

    @Test
    void rejectsInvalidSignature() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        var result = adapter.verifyWebhook(new SepayWebhookCommand(
                body, "sha256=invalid", timestamp));

        assertFalse(result.authenticated());
    }

    private String sign(String timestamp, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }
}
