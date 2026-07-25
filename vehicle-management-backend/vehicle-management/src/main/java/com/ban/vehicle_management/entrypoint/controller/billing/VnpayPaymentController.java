package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.payment.mapper.PaymentApiMapper;
import com.ban.vehicle_management.application.billing.payment.model.command.CreateVnpayPaymentCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentResult;
import com.ban.vehicle_management.application.billing.payment.port.in.VnpayPaymentPortIn;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.request.CreateVnpayPaymentRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.VnpayPaymentResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/billing/invoices/{invoiceId}/payments/vnpay")
public class VnpayPaymentController {

    private final VnpayPaymentPortIn vnpayPaymentPortIn;
    private final PaymentApiMapper paymentApiMapper;

    public VnpayPaymentController(
            VnpayPaymentPortIn vnpayPaymentPortIn,
            PaymentApiMapper paymentApiMapper
    ) {
        this.vnpayPaymentPortIn = vnpayPaymentPortIn;
        this.paymentApiMapper = paymentApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VnpayPaymentResponse>> createPayment(
            @PathVariable UUID invoiceId,
            @RequestBody(required = false) CreateVnpayPaymentRequest request,
            HttpServletRequest httpRequest
    ) {
        CreateVnpayPaymentRequest resolvedRequest = request == null
                ? new CreateVnpayPaymentRequest(null, "vn")
                : request;
        VnpayPaymentResult result = vnpayPaymentPortIn.createPayment(
                invoiceId,
                new CreateVnpayPaymentCommand(
                        resolvedRequest.bankCode(),
                        resolvedRequest.locale(),
                        resolveClientIp(httpRequest)
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "VNPAY payment created successfully",
                paymentApiMapper.toResponse(result)
        ));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
