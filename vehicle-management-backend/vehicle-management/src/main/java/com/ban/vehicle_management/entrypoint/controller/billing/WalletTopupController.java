package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.wallet.port.in.WalletTopupPortIn;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletTopupOrder;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.request.CreateWalletTopupRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.response.WalletTopupResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/wallets/me/topups")
public class WalletTopupController {

    private final WalletTopupPortIn walletTopupPortIn;

    public WalletTopupController(WalletTopupPortIn walletTopupPortIn) {
        this.walletTopupPortIn = walletTopupPortIn;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WalletTopupResponse>> createTopup(
            @Valid @RequestBody CreateWalletTopupRequest request,
            HttpServletRequest httpRequest) {
        String key = request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString()
                : request.idempotencyKey().trim();
        WalletTopupOrder order = walletTopupPortIn.createTopup(
                request.amount(), key, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Top-up order created successfully", toResponse(order)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletTopupResponse>>> listMyTopups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<WalletTopupResponse> responses = walletTopupPortIn.listMyTopups(page, size).stream()
                .map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok("Fetched top-up orders successfully", responses));
    }

    @GetMapping("/{topupOrderId}")
    public ResponseEntity<ApiResponse<WalletTopupResponse>> getTopup(@PathVariable UUID topupOrderId) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched top-up order successfully",
                toResponse(walletTopupPortIn.getTopup(topupOrderId))));
    }

    private WalletTopupResponse toResponse(WalletTopupOrder order) {
        return new WalletTopupResponse(
                order.getTopupOrderId(), order.getWalletId(), order.getAmount(), order.getCurrency(),
                order.getStatus() == null ? null : order.getStatus().name(),
                order.getTransactionRef(), order.getPaymentUrl(), order.getExpiresAt(),
                order.getCompletedAt(), order.getIdempotencyKey());
    }
}
