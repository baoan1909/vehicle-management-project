package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.payment.mapper.PaymentApiMapper;
import com.ban.vehicle_management.application.billing.payment.port.in.PaymentPortIn;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.request.CreatePaymentRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.request.PaymentFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.PaymentResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
public class PaymentController {

    private final PaymentPortIn paymentPortIn;
    private final PaymentApiMapper paymentApiMapper;

    public PaymentController(
            PaymentPortIn paymentPortIn,
            PaymentApiMapper paymentApiMapper
    ) {
        this.paymentPortIn = paymentPortIn;
        this.paymentApiMapper = paymentApiMapper;
    }

    @PostMapping("/invoices/{invoiceId}/payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @PathVariable UUID invoiceId,
            @RequestBody CreatePaymentRequest request
    ) {
        Payment payment = paymentPortIn.recordPayment(invoiceId, paymentApiMapper.toDomain(request));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Payment recorded successfully",
                paymentApiMapper.toResponse(payment)
        ));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPayments(
            @ModelAttribute PaymentFilterRequest request
    ) {
        List<Payment> payments = paymentPortIn.getPayments(
                request.invoiceId(),
                request.paymentMethod(),
                request.status(),
                request.receivedBy(),
                request.fromDate(),
                request.toDate(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched payments successfully",
                paymentApiMapper.toResponses(payments)
        ));
    }
}