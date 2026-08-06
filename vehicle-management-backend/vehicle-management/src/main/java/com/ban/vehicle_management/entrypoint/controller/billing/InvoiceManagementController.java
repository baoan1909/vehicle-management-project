package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.invoice.mapper.InvoiceApiMapper;
import com.ban.vehicle_management.application.billing.invoice.port.in.InvoiceManagementPortIn;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.request.InvoiceManagementFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceManagementDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceManagementPageResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceManagementSummaryResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/invoice-management")
public class InvoiceManagementController {

    private final InvoiceManagementPortIn invoiceManagementPortIn;
    private final InvoiceApiMapper invoiceApiMapper;

    public InvoiceManagementController(
            InvoiceManagementPortIn invoiceManagementPortIn,
            InvoiceApiMapper invoiceApiMapper
    ) {
        this.invoiceManagementPortIn = invoiceManagementPortIn;
        this.invoiceApiMapper = invoiceApiMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<InvoiceManagementPageResponse>> getInvoices(
            @ModelAttribute InvoiceManagementFilterRequest request
    ) {
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 10 : request.size();

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched invoice management list successfully",
                invoiceApiMapper.toManagementPageResponse(invoiceManagementPortIn.getInvoices(
                        request.status(),
                        request.paymentMethod(),
                        request.fromDate(),
                        request.toDate(),
                        request.keyword(),
                        page,
                        size
                ))
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<InvoiceManagementSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched invoice management summary successfully",
                invoiceApiMapper.toManagementSummaryResponse(invoiceManagementPortIn.getSummary())
        ));
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceManagementDetailResponse>> getInvoiceDetail(
            @PathVariable UUID invoiceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched invoice management detail successfully",
                invoiceApiMapper.toManagementDetailResponse(invoiceManagementPortIn.getInvoiceDetail(invoiceId))
        ));
    }
}
