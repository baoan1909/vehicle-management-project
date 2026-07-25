package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.payment.model.command.VnpayCallbackCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayIpnResult;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayReturnResult;
import com.ban.vehicle_management.application.billing.payment.port.in.VnpayPaymentPortIn;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.VnpayIpnResponse;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/api/public/payments/vnpay")
public class VnpayCallbackController {

    private final VnpayPaymentPortIn vnpayPaymentPortIn;
    private final String frontendReturnUrl;

    public VnpayCallbackController(
            VnpayPaymentPortIn vnpayPaymentPortIn,
            @Value("${app.payment.vnpay.frontend-return-url:http://localhost:5173/admin/swipe}")
            String frontendReturnUrl
    ) {
        this.vnpayPaymentPortIn = vnpayPaymentPortIn;
        this.frontendReturnUrl = frontendReturnUrl;
    }

    @GetMapping("/ipn")
    public ResponseEntity<VnpayIpnResponse> processIpn(
            @RequestParam Map<String, String> parameters
    ) {
        try {
            VnpayIpnResult result = vnpayPaymentPortIn.processIpn(new VnpayCallbackCommand(parameters));
            return ResponseEntity.ok(new VnpayIpnResponse(result.responseCode(), result.message()));
        } catch (Exception exception) {
            log.error("Cannot process VNPAY IPN", exception);
            return ResponseEntity.ok(new VnpayIpnResponse("99", "Unknown error"));
        }
    }

    @GetMapping("/return")
    public ResponseEntity<Void> verifyReturn(
            @RequestParam Map<String, String> parameters
    ) {
        try {
            VnpayReturnResult result = vnpayPaymentPortIn.verifyReturn(
                    new VnpayCallbackCommand(parameters)
            );
            return redirectToFrontend(
                    resolveFrontendResult(result),
                    result.transactionRef(),
                    result.responseCode(),
                    result.paymentStatus() == null ? null : result.paymentStatus().name()
            );
        } catch (Exception exception) {
            log.error("Cannot process VNPAY return callback", exception);
            return redirectToFrontend(
                    "failed",
                    parameters.get("vnp_TxnRef"),
                    "INTERNAL_ERROR",
                    "PENDING"
            );
        }
    }

    private ResponseEntity<Void> redirectToFrontend(
            String resultCode,
            String transactionRef,
            String responseCode,
            String paymentStatus
    ) {
        URI redirectUri = UriComponentsBuilder.fromUriString(frontendReturnUrl)
                .queryParam("vnpayResult", resultCode)
                .queryParam("transactionRef", transactionRef)
                .queryParam("responseCode", responseCode)
                .queryParam("paymentStatus", paymentStatus)
                .build()
                .encode()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    private String resolveFrontendResult(VnpayReturnResult result) {
        if (!result.validSignature()) {
            return "invalid";
        }
        if (result.successful() && PaymentStatus.SUCCESS.equals(result.paymentStatus())) {
            return "success";
        }
        if ("24".equals(result.responseCode())) {
            return "cancelled";
        }
        return "failed";
    }
}
