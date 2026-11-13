package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.payment.model.command.SepayWebhookCommand;
import com.ban.vehicle_management.application.billing.payment.port.in.SepayPaymentPortIn;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/payments/sepay")
public class SepayWebhookController {
    private final SepayPaymentPortIn sepay;

    public SepayWebhookController(SepayPaymentPortIn sepay) {
        this.sepay = sepay;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Boolean>> webhook(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-SePay-Signature", required = false) String signature,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestamp) {
        sepay.processWebhook(new SepayWebhookCommand(rawBody, signature, timestamp));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
