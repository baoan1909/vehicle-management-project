package com.ban.vehicle_management.infrastructure.payment.vnpay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.billing.payment.model.VnpayPaymentRequest;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayCallbackData;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentLink;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VnpayGatewayAdapterTest {

    private VnpayGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        VnpayProperties properties = new VnpayProperties();
        properties.setEnabled(true);
        properties.setTmnCode("TESTCODE");
        properties.setHashSecret("test-secret");
        properties.setReturnUrl("https://merchant.example/vnpay/return");
        properties.setTimeoutMinutes(15);
        adapter = new VnpayGatewayAdapter(properties);
    }

    @Test
    void shouldCreateVerifiablePaymentUrl() {
        VnpayPaymentLink link = adapter.createPaymentLink(new VnpayPaymentRequest(
                "VNP123",
                new BigDecimal("50000.00"),
                "Thanh toan hoa don INV2026001",
                "127.0.0.1",
                "NCB",
                "vn",
                Instant.parse("2026-07-25T02:00:00Z")
        ));

        Map<String, String> parameters = parseQuery(link.paymentUrl());
        VnpayCallbackData callback = adapter.verifyCallback(parameters);

        assertTrue(callback.validSignature());
        assertEquals(new BigDecimal("50000.00"), callback.amount());
        assertTrue(link.paymentUrl().contains("vnp_Amount=5000000"));
        assertTrue(link.paymentUrl().contains("vnp_BankCode=NCB"));
    }

    @Test
    void shouldRejectTamperedCallback() {
        VnpayPaymentLink link = adapter.createPaymentLink(new VnpayPaymentRequest(
                "VNP456",
                new BigDecimal("80000.00"),
                "Thanh toan hoa don INV2026002",
                "127.0.0.1",
                null,
                "vn",
                Instant.parse("2026-07-25T02:00:00Z")
        ));

        Map<String, String> parameters = parseQuery(link.paymentUrl());
        parameters.put("vnp_Amount", "100");

        assertFalse(adapter.verifyCallback(parameters).validSignature());
    }

    private Map<String, String> parseQuery(String url) {
        return Arrays.stream(URI.create(url).getRawQuery().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> decode(pair[0]),
                        pair -> pair.length > 1 ? decode(pair[1]) : ""
                ));
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
