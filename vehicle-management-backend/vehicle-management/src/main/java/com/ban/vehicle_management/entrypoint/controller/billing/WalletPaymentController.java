package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.wallet.port.in.WalletPaymentPortIn;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.request.PayInvoiceByWalletRequest;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/wallets/me/payments")
public class WalletPaymentController {

    private final WalletPaymentPortIn walletPaymentPortIn;

    public WalletPaymentController(WalletPaymentPortIn walletPaymentPortIn) {
        this.walletPaymentPortIn = walletPaymentPortIn;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Payment>> payInvoice(
            @Valid @RequestBody PayInvoiceByWalletRequest request) {
        String key = request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString()
                : request.idempotencyKey().trim();
        Payment payment = walletPaymentPortIn.payInvoice(request.invoiceId(), key);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Invoice paid by wallet successfully", payment));
    }
}
