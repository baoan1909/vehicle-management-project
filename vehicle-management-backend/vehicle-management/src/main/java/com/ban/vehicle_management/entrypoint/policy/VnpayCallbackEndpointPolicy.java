package com.ban.vehicle_management.entrypoint.policy;

import com.ban.vehicle_management.application.billing.payment.mapper.PaymentApiMapper;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayReturnResult;
import com.ban.vehicle_management.application.billing.payment.port.in.VnpayPaymentPortIn;
import com.ban.vehicle_management.entrypoint.converter.VnpayCallbackRequestConverter;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.VnpayIpnResponse;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class VnpayCallbackEndpointPolicy {

    private final VnpayPaymentPortIn vnpayPaymentPortIn;
    private final VnpayCallbackRequestConverter requestConverter;
    private final PaymentApiMapper paymentApiMapper;
    private final String frontendReturnUrl;

    public VnpayCallbackEndpointPolicy(
            VnpayPaymentPortIn vnpayPaymentPortIn,
            VnpayCallbackRequestConverter requestConverter,
            PaymentApiMapper paymentApiMapper,
            @Value("${app.payment.vnpay.frontend-return-url:http://localhost:5173/payment/vnpay-return}")
            String frontendReturnUrl) {
        this.vnpayPaymentPortIn = vnpayPaymentPortIn;
        this.requestConverter = requestConverter;
        this.paymentApiMapper = paymentApiMapper;
        this.frontendReturnUrl = frontendReturnUrl;
    }

    public ResponseEntity<VnpayIpnResponse> processIpn(HttpServletRequest request) {
        try {
            return ResponseEntity.ok(paymentApiMapper.toResponse(
                    vnpayPaymentPortIn.processIpn(requestConverter.toCommand(request))));
        } catch (Exception exception) {
            log.error("Cannot process VNPAY IPN", exception);
            return ResponseEntity.ok(new VnpayIpnResponse("99", "Unknown error"));
        }
    }

    public ResponseEntity<Void> verifyReturn(HttpServletRequest request) {
        try {
            VnpayReturnResult result = vnpayPaymentPortIn.verifyReturn(requestConverter.toCommand(request));
            return redirect(resolveFrontendResult(result), result.transactionRef(), result.responseCode(),
                    result.paymentStatus() == null ? null : result.paymentStatus().name());
        } catch (Exception exception) {
            log.error("Cannot process VNPAY return callback", exception);
            return redirect("failed", requestConverter.transactionRef(request), "INTERNAL_ERROR", "PENDING");
        }
    }

    private ResponseEntity<Void> redirect(
            String resultCode, String transactionRef, String responseCode, String paymentStatus) {
        URI redirectUri = UriComponentsBuilder.fromUriString(frontendReturnUrl)
                .queryParam("vnpayResult", resultCode)
                .queryParam("transactionRef", transactionRef)
                .queryParam("responseCode", responseCode)
                .queryParam("paymentStatus", paymentStatus)
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    private String resolveFrontendResult(VnpayReturnResult result) {
        if (!result.validSignature()) {
            return "invalid";
        }
        if (result.successful() && PaymentStatus.SUCCESS.equals(result.paymentStatus())) {
            return "success";
        }
        return "24".equals(result.responseCode()) ? "cancelled" : "failed";
    }
}
