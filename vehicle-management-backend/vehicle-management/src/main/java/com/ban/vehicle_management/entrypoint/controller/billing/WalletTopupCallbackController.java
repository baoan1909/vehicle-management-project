package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.wallet.port.in.WalletTopupPortIn;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayIpnResult;
import com.ban.vehicle_management.entrypoint.converter.VnpayCallbackRequestConverter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public VNPAY IPN/return for wallet top-ups. No authentication: signature is the trust anchor.
 */
@RestController
@RequestMapping("/api/public/wallet-topups/vnpay")
public class WalletTopupCallbackController {

    private final WalletTopupPortIn walletTopupPortIn;
    private final VnpayCallbackRequestConverter requestConverter;

    public WalletTopupCallbackController(
            WalletTopupPortIn walletTopupPortIn,
            VnpayCallbackRequestConverter requestConverter) {
        this.walletTopupPortIn = walletTopupPortIn;
        this.requestConverter = requestConverter;
    }

    @GetMapping("/ipn")
    public ResponseEntity<VnpayIpnResult> ipn(HttpServletRequest request) {
        return ResponseEntity.ok(walletTopupPortIn.processIpn(requestConverter.toCommand(request)));
    }

    @GetMapping("/return")
    public ResponseEntity<?> vnpayReturn(HttpServletRequest request) {
        return ResponseEntity.ok(walletTopupPortIn.verifyReturn(requestConverter.toCommand(request)));
    }
}
