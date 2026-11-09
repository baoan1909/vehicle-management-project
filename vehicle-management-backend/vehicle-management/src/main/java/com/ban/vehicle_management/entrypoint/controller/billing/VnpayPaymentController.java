package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.payment.mapper.PaymentApiMapper;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentResult;
import com.ban.vehicle_management.application.billing.payment.port.in.VnpayPaymentPortIn;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.request.CreateVnpayPaymentRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.VnpayPaymentResponse;
import com.ban.vehicle_management.entrypoint.converter.ClientIpResolver;
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

@RestController
@RequestMapping("/api/billing/invoices/{invoiceId}/payments/vnpay")
public class VnpayPaymentController {

    private final VnpayPaymentPortIn vnpayPaymentPortIn;
    private final PaymentApiMapper paymentApiMapper;
    private final ClientIpResolver clientIpResolver;

    public VnpayPaymentController(
            VnpayPaymentPortIn vnpayPaymentPortIn,
            PaymentApiMapper paymentApiMapper,
            ClientIpResolver clientIpResolver
    ) {
        this.vnpayPaymentPortIn = vnpayPaymentPortIn;
        this.paymentApiMapper = paymentApiMapper;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VnpayPaymentResponse>> createPayment(
            @PathVariable UUID invoiceId,
            @RequestBody(required = false) CreateVnpayPaymentRequest request,
            HttpServletRequest httpRequest
    ) {
        VnpayPaymentResult result = vnpayPaymentPortIn.createPayment(
                invoiceId,
                paymentApiMapper.toCommand(request, clientIpResolver.resolve(httpRequest))
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "VNPAY payment created successfully",
                paymentApiMapper.toResponse(result)
        ));
    }

}
