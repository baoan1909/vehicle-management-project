package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.SepayWebhookResponse;
import com.ban.vehicle_management.entrypoint.policy.SepayWebhookEndpointPolicy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/payments/sepay")
public class SepayWebhookController {
    private final SepayWebhookEndpointPolicy endpointPolicy;

    public SepayWebhookController(SepayWebhookEndpointPolicy endpointPolicy) {
        this.endpointPolicy = endpointPolicy;
    }

    @PostMapping("/webhook")
    public ResponseEntity<SepayWebhookResponse> webhook(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-SePay-Signature", required = false) String signature,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestamp) {
        return endpointPolicy.process(rawBody, signature, timestamp);
    }
}
