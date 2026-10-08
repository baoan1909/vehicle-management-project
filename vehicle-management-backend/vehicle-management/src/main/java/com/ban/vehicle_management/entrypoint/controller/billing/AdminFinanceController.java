package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.wallet.port.in.SettlementPortIn;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/admin/finance")
public class AdminFinanceController {

    private final SettlementPortIn settlementPortIn;

    public AdminFinanceController(SettlementPortIn settlementPortIn) {
        this.settlementPortIn = settlementPortIn;
    }

    @PostMapping("/allocations")
    public ResponseEntity<ApiResponse<?>> allocate(
            @RequestParam UUID paymentId,
            @RequestParam(required = false) UUID voucherId) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Allocation created successfully",
                settlementPortIn.allocateForWalletPayment(paymentId, voucherId)));
    }

    @PostMapping("/settlements/release")
    public ResponseEntity<ApiResponse<?>> release(@RequestParam(defaultValue = "200") int batchSize) {
        int released = settlementPortIn.releaseDueSettlements(batchSize);
        return ResponseEntity.ok(ApiResponse.ok("Released " + released + " settlements", released));
    }

    @PatchMapping("/payouts/{payoutId}/approve")
    public ResponseEntity<ApiResponse<?>> approve(@PathVariable UUID payoutId) {
        return ResponseEntity.ok(ApiResponse.ok("Payout approved successfully",
                settlementPortIn.approvePayout(payoutId)));
    }

    @PatchMapping("/payouts/{payoutId}/reject")
    public ResponseEntity<ApiResponse<?>> reject(
            @PathVariable UUID payoutId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Payout rejected successfully",
                settlementPortIn.rejectPayout(payoutId, reason)));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<?>> overview() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched financial overview successfully",
                settlementPortIn.financialOverview()));
    }

    @GetMapping("/reconciliation/topup-exceptions")
    public ResponseEntity<ApiResponse<?>> topupExceptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched reconciliation exceptions successfully",
                settlementPortIn.financialOverview()));
    }
}
