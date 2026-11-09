package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.wallet.port.in.WalletTopupPortIn;
import com.ban.vehicle_management.application.billing.wallet.mapper.WalletApiMapper;
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
    private final WalletApiMapper walletApiMapper;

    public WalletTopupController(WalletTopupPortIn walletTopupPortIn, WalletApiMapper walletApiMapper) {
        this.walletTopupPortIn = walletTopupPortIn;
        this.walletApiMapper = walletApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WalletTopupResponse>> createTopup(
            @Valid @RequestBody CreateWalletTopupRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Top-up order created successfully", walletApiMapper.toTopupResponse(
                        walletTopupPortIn.createTopup(
                                request.amount(), request.idempotencyKey(), httpRequest.getRemoteAddr()))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletTopupResponse>>> listMyTopups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched top-up orders successfully",
                walletApiMapper.toTopupResponses(walletTopupPortIn.listMyTopups(page, size))));
    }

    @GetMapping("/{topupOrderId}")
    public ResponseEntity<ApiResponse<WalletTopupResponse>> getTopup(@PathVariable UUID topupOrderId) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched top-up order successfully",
                walletApiMapper.toTopupResponse(walletTopupPortIn.getTopup(topupOrderId))));
    }
}
