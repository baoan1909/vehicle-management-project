package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.payment.model.command.VnpayCallbackCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayIpnResult;
import com.ban.vehicle_management.application.billing.wallet.port.in.WalletTopupPortIn;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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

    public WalletTopupCallbackController(WalletTopupPortIn walletTopupPortIn) {
        this.walletTopupPortIn = walletTopupPortIn;
    }

    @GetMapping("/ipn")
    public ResponseEntity<VnpayIpnResult> ipn(HttpServletRequest request) {
        VnpayIpnResult result = walletTopupPortIn.processIpn(new VnpayCallbackCommand(extractParams(request)));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/return")
    public ResponseEntity<?> vnpayReturn(HttpServletRequest request) {
        var result = walletTopupPortIn.verifyReturn(new VnpayCallbackCommand(extractParams(request)));
        return ResponseEntity.ok(result);
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }
}
