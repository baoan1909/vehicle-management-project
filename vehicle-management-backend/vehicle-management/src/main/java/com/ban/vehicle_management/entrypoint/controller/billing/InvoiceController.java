package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.invoice.mapper.InvoiceApiMapper;
import com.ban.vehicle_management.application.billing.invoice.port.in.InvoicePortIn;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.model.InvoiceDetail;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.request.CreateInvoiceRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.request.InvoiceFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceDetailResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing/invoices")
public class InvoiceController {

    private final InvoicePortIn invoicePortIn;
    private final InvoiceApiMapper invoiceApiMapper;

    public  InvoiceController(
            InvoicePortIn invoicePortIn,
            InvoiceApiMapper invoiceApiMapper
    ){
        this.invoicePortIn = invoicePortIn;
        this.invoiceApiMapper = invoiceApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceAdminResponse>> createInvoice(
            @RequestBody CreateInvoiceRequest request
            ){
        Invoice createdInvoice = invoicePortIn.createInvoice(invoiceApiMapper.toDomain(request));
        return  ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Invoice created successfully",
                invoiceApiMapper.toAdminResponse(createdInvoice)
        ));
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceDetailResponse>> getInvoiceById(
            @PathVariable UUID invoiceId
    ) {
        InvoiceDetail invoiceDetail = invoicePortIn.getInvoiceById(invoiceId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched invoice successfully",
                invoiceApiMapper.toDetailResponse(invoiceDetail)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceAdminResponse>>> getInvoices(
            @ModelAttribute InvoiceFilterRequest request
    ) {
        List<Invoice> invoices = invoicePortIn.getInvoices(
                request.customerId(),
                request.parkingSessionId(),
                request.subscriptionId(),
                request.lostCardReportId(),
                request.status(),
                request.fromDate(),
                request.toDate(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched invoices successfully",
                invoiceApiMapper.toAdminResponses(invoices)
        ));
    }

    @PatchMapping("/{invoiceId}/cancel")
    public ResponseEntity<ApiResponse<InvoiceAdminResponse>> cancelInvoice(
            @PathVariable UUID invoiceId
    ) {
        Invoice cancelledInvoice = invoicePortIn.cancelInvoice(invoiceId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Invoice cancelled successfully",
                invoiceApiMapper.toAdminResponse(cancelledInvoice)
        ));
    }

}
