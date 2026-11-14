package com.ban.vehicle_management.entrypoint.policy;

import com.ban.vehicle_management.application.billing.payment.model.command.SepayWebhookCommand;
import com.ban.vehicle_management.application.billing.payment.port.in.SepayPaymentPortIn;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.SepayWebhookResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class SepayWebhookEndpointPolicy {
    private final SepayPaymentPortIn sepay;

    public SepayWebhookEndpointPolicy(SepayPaymentPortIn sepay) {
        this.sepay = sepay;
    }

    public ResponseEntity<SepayWebhookResponse> process(
            byte[] rawBody,
            String signature,
            String timestamp) {
        sepay.processWebhook(new SepayWebhookCommand(rawBody, signature, timestamp));
        return ResponseEntity.ok(new SepayWebhookResponse(true));
    }
}
