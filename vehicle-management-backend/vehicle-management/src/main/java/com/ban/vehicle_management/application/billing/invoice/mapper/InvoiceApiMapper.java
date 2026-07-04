package com.ban.vehicle_management.application.billing.invoice.mapper;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.model.InvoiceDetail;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.request.CreateInvoiceRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceDetailResponse;
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
}
