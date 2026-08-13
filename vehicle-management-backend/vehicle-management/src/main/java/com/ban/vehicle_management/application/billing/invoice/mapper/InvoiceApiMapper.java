package com.ban.vehicle_management.application.billing.invoice.mapper;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.model.InvoiceDetail;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceLineItemResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementDetailResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementItemResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementPageResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementSummaryResult;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.request.CreateInvoiceRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceLineItemResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceManagementDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceManagementItemResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceManagementPageResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceManagementSummaryResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.PaymentResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceApiMapper {

    @Mapping(target = "invoiceId", ignore = true)
    @Mapping(target = "invoiceNo", ignore = true)
    @Mapping(target = "finalAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "issuedAt", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Invoice toDomain(CreateInvoiceRequest request);

    InvoiceAdminResponse toAdminResponse(Invoice invoice);

    List<InvoiceAdminResponse> toAdminResponses(List<Invoice> invoices);

    default  String map(Instant instant){
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }

    PaymentResponse toPaymentResponse(Payment payment);

    List<PaymentResponse> toPaymentResponses(List<Payment> payments);

    default InvoiceDetailResponse toDetailResponse(InvoiceDetail detail) {
        Invoice invoice = detail.getInvoice();

        return new InvoiceDetailResponse(
                invoice.getInvoiceId(),
                invoice.getInvoiceNo(),
                invoice.getCustomerId(),
                invoice.getParkingSessionId(),
                invoice.getSubscriptionId(),
                invoice.getLostCardReportId(),
                invoice.getAmount(),
                invoice.getDiscountAmount(),
                invoice.getFinalAmount(),
                invoice.getStatus(),
                map(invoice.getIssuedAt()),
                map(invoice.getPaidAt()),
                map(invoice.getCreatedAt()),
                invoice.getCreatedBy(),
                map(invoice.getUpdatedAt()),
                invoice.getUpdatedBy(),
                toPaymentResponses(detail.getPayments())
        );
    }

    default InvoiceManagementItemResponse toManagementItemResponse(InvoiceManagementItemResult item) {
        return new InvoiceManagementItemResponse(
                item.invoiceId(),
                item.invoiceNo(),
                item.customerId(),
                item.customerName(),
                item.licensePlate(),
                item.source(),
                item.sourceId(),
                item.amount(),
                item.discountAmount(),
                item.finalAmount(),
                item.status(),
                item.paymentMethod(),
                item.paymentStatus(),
                item.transactionRef(),
                map(item.issuedAt()),
                map(item.paidAt()),
                map(item.createdAt()),
                map(item.updatedAt())
        );
    }

    default InvoiceManagementPageResponse toManagementPageResponse(InvoiceManagementPageResult result) {
        return new InvoiceManagementPageResponse(
                result.items().stream().map(this::toManagementItemResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    default InvoiceManagementSummaryResponse toManagementSummaryResponse(InvoiceManagementSummaryResult result) {
        return new InvoiceManagementSummaryResponse(
                result.total(),
                result.unpaid(),
                result.paid(),
                result.cancelled(),
                result.refunded()
        );
    }

    default InvoiceLineItemResponse toLineItemResponse(InvoiceLineItemResult item) {
        return new InvoiceLineItemResponse(item.code(), item.description(), item.amount());
    }

    default InvoiceManagementDetailResponse toManagementDetailResponse(InvoiceManagementDetailResult result) {
        return new InvoiceManagementDetailResponse(
                toManagementItemResponse(result.invoice()),
                result.lineItems().stream().map(this::toLineItemResponse).toList(),
                toPaymentResponses(result.payments())
        );
    }
}
