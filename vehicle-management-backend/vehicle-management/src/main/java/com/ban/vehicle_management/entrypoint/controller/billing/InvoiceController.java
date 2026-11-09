package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.invoice.mapper.InvoiceApiMapper;
import com.ban.vehicle_management.application.billing.invoice.port.in.InvoicePortIn;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.model.InvoiceDetail;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.request.CreateInvoiceRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.request.InvoiceFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.request.InvoiceManagementFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceUserResponse;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.time.Instant;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceManagementDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceManagementPageResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceManagementSummaryResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import com.ban.vehicle_management.entrypoint.converter.PaginationRequestConverter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing/invoices")
public class InvoiceController {

    private final InvoicePortIn invoicePortIn;
    private final InvoiceApiMapper invoiceApiMapper;
    private final PaginationRequestConverter paginationRequestConverter;

    public  InvoiceController(
            InvoicePortIn invoicePortIn,
            InvoiceApiMapper invoiceApiMapper,
            PaginationRequestConverter paginationRequestConverter
    ){
        this.invoicePortIn = invoicePortIn;
        this.invoiceApiMapper = invoiceApiMapper;
        this.paginationRequestConverter = paginationRequestConverter;
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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<InvoiceUserResponse>>> getMyInvoices(
            @RequestParam(required = false) UUID subscriptionId,
            @RequestParam(required = false) List<UUID> subscriptionIds,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit
    ) {
        List<Invoice> invoices = invoicePortIn.getOwnInvoices(
                subscriptionId,
                subscriptionIds,
                status,
                fromDate,
                toDate,
                keyword,
                limit
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched current customer invoices successfully",
                invoiceApiMapper.toUserResponses(invoices)
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

    @GetMapping("/management")
    public ResponseEntity<ApiResponse<InvoiceManagementPageResponse>> getManagementInvoices(
            @ModelAttribute InvoiceManagementFilterRequest request
    ) {
        var pagination = paginationRequestConverter.resolve(request.page(), request.size());

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched invoice management list successfully",
                invoiceApiMapper.toManagementPageResponse(invoicePortIn.getManagementInvoices(
                        request.status(),
                        request.paymentMethod(),
                        request.fromDate(),
                        request.toDate(),
                        request.keyword(),
                        pagination.page(),
                        pagination.size(),
                        request.organizationId(),
                        request.parkingLotId()
                ))
        ));
    }

    @GetMapping("/management/summary")
    public ResponseEntity<ApiResponse<InvoiceManagementSummaryResponse>> getManagementSummary(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID parkingLotId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched invoice management summary successfully",
                invoiceApiMapper.toManagementSummaryResponse(invoicePortIn.getManagementSummary(organizationId, parkingLotId))
        ));
    }

    @GetMapping("/management/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceManagementDetailResponse>> getManagementInvoiceDetail(
            @PathVariable UUID invoiceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched invoice management detail successfully",
                invoiceApiMapper.toManagementDetailResponse(invoicePortIn.getManagementInvoiceDetail(invoiceId))
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
