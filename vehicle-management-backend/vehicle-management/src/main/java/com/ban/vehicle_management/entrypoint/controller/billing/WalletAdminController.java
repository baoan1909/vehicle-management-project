package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.wallet.mapper.WalletApiMapper;
import com.ban.vehicle_management.application.billing.wallet.port.in.WalletAdminPortIn;
import com.ban.vehicle_management.application.billing.wallet.port.in.WalletPortIn;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.request.CreateWalletAdjustmentRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.request.RejectWalletAdjustmentRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.response.WalletAdjustmentResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.response.WalletResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.response.WalletTransactionResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/admin")
public class WalletAdminController {

    private final WalletPortIn walletPortIn;
    private final WalletAdminPortIn walletAdminPortIn;
    private final WalletApiMapper walletApiMapper;

    public WalletAdminController(
            WalletPortIn walletPortIn,
            WalletAdminPortIn walletAdminPortIn,
            WalletApiMapper walletApiMapper) {
        this.walletPortIn = walletPortIn;
        this.walletAdminPortIn = walletAdminPortIn;
        this.walletApiMapper = walletApiMapper;
    }

    @GetMapping("/wallets")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> listWallets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched wallets successfully",
                walletApiMapper.toResponses(walletPortIn.listWallets(page, size))));
    }

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable UUID walletId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched wallet successfully",
                walletApiMapper.toResponse(walletPortIn.getWalletById(walletId))));
    }

    @GetMapping("/wallets/{walletId}/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getTransactions(
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched wallet transactions successfully",
                walletApiMapper.toTransactionResponses(
                        walletPortIn.getWalletTransactions(walletId, page, size))));
    }

    @PatchMapping("/wallets/{walletId}/lock")
    public ResponseEntity<ApiResponse<Void>> lockWallet(@PathVariable UUID walletId) {
        walletAdminPortIn.lockWallet(walletId);
        return ResponseEntity.ok(ApiResponse.ok("Wallet locked successfully", null));
    }

    @PatchMapping("/wallets/{walletId}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockWallet(@PathVariable UUID walletId) {
        walletAdminPortIn.unlockWallet(walletId);
        return ResponseEntity.ok(ApiResponse.ok("Wallet unlocked successfully", null));
    }

    @PostMapping("/wallet-adjustments")
    public ResponseEntity<ApiResponse<WalletAdjustmentResponse>> requestAdjustment(
            @RequestBody CreateWalletAdjustmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Adjustment requested successfully",
                walletApiMapper.toResponse(walletAdminPortIn.requestAdjustment(
                        request.walletId(), request.amount(), request.direction(), request.reason()))));
    }

    @PatchMapping("/wallet-adjustments/{adjustmentId}/approve")
    public ResponseEntity<ApiResponse<WalletAdjustmentResponse>> approveAdjustment(
            @PathVariable UUID adjustmentId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Adjustment approved successfully",
                walletApiMapper.toResponse(walletAdminPortIn.approveAdjustment(adjustmentId))));
    }

    @PatchMapping("/wallet-adjustments/{adjustmentId}/reject")
    public ResponseEntity<ApiResponse<WalletAdjustmentResponse>> rejectAdjustment(
            @PathVariable UUID adjustmentId,
            @RequestBody(required = false) RejectWalletAdjustmentRequest request) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(ApiResponse.ok(
                "Adjustment rejected successfully",
                walletApiMapper.toResponse(walletAdminPortIn.rejectAdjustment(adjustmentId, reason))));
    }
}
