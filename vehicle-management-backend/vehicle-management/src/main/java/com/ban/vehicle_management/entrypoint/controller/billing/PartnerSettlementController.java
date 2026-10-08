package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.wallet.port.in.SettlementPortIn;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/partner-wallets/current")
public class PartnerSettlementController {

    private final SettlementPortIn settlementPortIn;

    public PartnerSettlementController(SettlementPortIn settlementPortIn) {
        this.settlementPortIn = settlementPortIn;
    }

    @GetMapping("/allocations")
    public ResponseEntity<ApiResponse<?>> listAllocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched allocations successfully",
                settlementPortIn.listMyAllocations(page, size)));
    }

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<?>> listPayouts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched payouts successfully",
                settlementPortIn.listMyPayouts(page, size)));
    }

    @PostMapping("/payouts")
    public ResponseEntity<ApiResponse<?>> requestPayout(
            @RequestParam UUID bankAccountId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String idempotencyKey) {
        String key = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString() : idempotencyKey.trim();
        return ResponseEntity.status(201).body(ApiResponse.ok("Payout requested successfully",
                settlementPortIn.requestPayout(bankAccountId, amount, key)));
    }
}
