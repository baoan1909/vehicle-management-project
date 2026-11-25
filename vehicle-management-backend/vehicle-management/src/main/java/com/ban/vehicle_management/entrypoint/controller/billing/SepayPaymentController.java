package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.payment.mapper.PaymentApiMapper;
import com.ban.vehicle_management.application.billing.payment.port.in.SepayPaymentPortIn;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.SepayPaymentResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/invoices/{invoiceId}/payments/sepay")
public class SepayPaymentController {

    private final SepayPaymentPortIn sepay;
    private final PaymentApiMapper mapper;

    public SepayPaymentController(SepayPaymentPortIn sepay, PaymentApiMapper mapper) {
        this.sepay = sepay;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SepayPaymentResponse>> createPayment(@PathVariable UUID invoiceId) {
        var result = sepay.createParkingCheckoutPayment(invoiceId);
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok("SePay payment created successfully", mapper.toResponse(result)));
    }
}
