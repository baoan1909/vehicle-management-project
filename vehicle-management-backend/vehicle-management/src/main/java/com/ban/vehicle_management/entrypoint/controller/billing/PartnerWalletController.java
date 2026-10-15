package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.wallet.mapper.WalletApiMapper;
import com.ban.vehicle_management.application.billing.wallet.port.in.WalletPortIn;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.response.WalletResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.response.WalletTransactionResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/partner-wallets")
public class PartnerWalletController {

    private final WalletPortIn walletPortIn;
    private final WalletApiMapper walletApiMapper;

    public PartnerWalletController(WalletPortIn walletPortIn, WalletApiMapper walletApiMapper) {
        this.walletPortIn = walletPortIn;
        this.walletApiMapper = walletApiMapper;
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<WalletResponse>> getCurrentPartnerWallet() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched partner wallet successfully",
                walletApiMapper.toResponse(walletPortIn.getCurrentPartnerWallet())));
    }

    @GetMapping("/current/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getCurrentPartnerTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched partner wallet transactions successfully",
                walletApiMapper.toTransactionResponses(
                        walletPortIn.getCurrentPartnerTransactions(page, size))));
    }
}
