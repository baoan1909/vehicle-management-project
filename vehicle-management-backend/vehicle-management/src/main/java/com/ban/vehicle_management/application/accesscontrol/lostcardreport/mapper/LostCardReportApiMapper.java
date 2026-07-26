package com.ban.vehicle_management.application.accesscontrol.lostcardreport.mapper;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardPreviewResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportDetailResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportListItemResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportSummaryResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportWorkflowResult;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.billing.invoice.model.InvoiceDetail;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.request.CreateLostCardReportRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardPreviewResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardReportDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardReportListItemResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardReportResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardReportSummaryResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response.LostCardReportWorkflowResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response.SubscriptionAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.PaymentResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LostCardReportApiMapper {

    @Mapping(target = "lostCardReportId", ignore = true)
    @Mapping(target = "cardId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "notificationTime", ignore = true)
    @Mapping(target = "ticketPrice", ignore = true)
    @Mapping(target = "lostCardFee", ignore = true)
    @Mapping(target = "context", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "resolvedBy", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    LostCardReport toDomain(CreateLostCardReportRequest request);

    LostCardReportResponse toResponse(LostCardReport report);

    List<LostCardReportResponse> toResponses(List<LostCardReport> reports);

    default LostCardReportListItemResponse toListItemResponse(LostCardReportListItemResult item) {
        if (item == null) {
            return null;
        }

        return new LostCardReportListItemResponse(
                item.lostCardReportId(),
                buildReportCode(item),
                item.cardId(),
                item.customerId(),
                item.parkingSessionId(),
                item.subscriptionId(),
                item.licensePlate(),
                map(item.notificationTime()),
                map(item.timeOfLost()),
                item.ticketPrice(),
                item.lostCardFee(),
                item.totalAmount(),
                item.reporterName(),
                item.reporterPhone(),
                item.identifyCard(),
                item.registrationLicense(),
                item.context(),
                item.status(),
                item.invoiceId(),
                item.invoiceNo(),
                item.invoiceStatus(),
                map(item.createdAt()),
                item.createdBy(),
                map(item.updatedAt()),
                item.updatedBy()
        );
    }

    default List<LostCardReportListItemResponse> toListItemResponses(List<LostCardReportListItemResult> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(this::toListItemResponse)
                .toList();
    }

    LostCardPreviewResponse toPreviewResponse(LostCardPreviewResult preview);

    LostCardReportWorkflowResponse toWorkflowResponse(LostCardReportWorkflowResult result);

    LostCardReportSummaryResponse toSummaryResponse(LostCardReportSummaryResult result);

    default LostCardReportDetailResponse toDetailResponse(LostCardReportDetailResult detail) {
        if (detail == null) {
            return null;
        }

        return new LostCardReportDetailResponse(
                toResponse(detail.lostCardReport()),
                detail.oldCardNumber(),
                detail.customerName(),
                detail.licensePlate(),
                toParkingSessionResponse(detail.parkingSession()),
                toSubscriptionResponse(detail.subscription()),
                toInvoiceDetailResponse(detail.invoiceDetail()),
                detail.checkInLicensePlateImagePath(),
                detail.checkInPersonImagePath()
        );
    }

    default String map(Instant value) {
        return DateTimeUtils.formatInstant(value, DateTimeUtils.VIETNAM_ZONE);
    }

    private String buildReportCode(LostCardReportListItemResult item) {
        String datePart = item.notificationTime() == null
                ? "UNKNOWN"
                : DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(DateTimeUtils.VIETNAM_ZONE)
                .format(item.notificationTime());
        String idPart = item.lostCardReportId() == null
                ? "UNKNOWN"
                : item.lostCardReportId().toString().replace("-", "").substring(0, 8).toUpperCase();

        return "LC-" + datePart + "-" + idPart;
    }

    private ParkingSessionResponse toParkingSessionResponse(ParkingSession parkingSession) {
        if (parkingSession == null) {
            return null;
        }

        ParkingSessionResponse response = new ParkingSessionResponse();
        response.setParkingSessionId(parkingSession.getParkingSessionId());
        response.setCardId(parkingSession.getCardId());
        response.setCustomerId(parkingSession.getCustomerId());
        response.setCustomerVehicleId(parkingSession.getCustomerVehicleId());
        response.setVehicleTypeId(parkingSession.getVehicleTypeId());
        response.setZoneId(parkingSession.getZoneId());
        response.setLicensePlateIn(parkingSession.getLicensePlateIn());
        response.setLicensePlateOut(parkingSession.getLicensePlateOut());
        response.setCheckInTime(map(parkingSession.getCheckInTime()));
        response.setCheckOutTime(map(parkingSession.getCheckOutTime()));
        response.setStatus(parkingSession.getStatus());
        response.setTotalPrice(parkingSession.getTotalPrice());
        return response;
    }

    private SubscriptionAdminResponse toSubscriptionResponse(Subscription subscription) {
        if (subscription == null) {
            return null;
        }

        SubscriptionAdminResponse response = new SubscriptionAdminResponse();
        response.setSubscriptionId(subscription.getSubscriptionId());
        response.setCustomerId(subscription.getCustomerId());
        response.setCustomerVehicleId(subscription.getCustomerVehicleId());
        response.setCardId(subscription.getCardId());
        response.setTicketTypeId(subscription.getTicketTypeId());
        response.setPriceRuleId(subscription.getPriceRuleId());
        response.setRequestedEffectiveFrom(subscription.getRequestedEffectiveFrom());
        response.setEffectiveFrom(subscription.getEffectiveFrom());
        response.setEffectiveTo(subscription.getEffectiveTo());
        response.setPrice(subscription.getPrice());
        response.setStatus(subscription.getStatus());
        response.setApprovedBy(subscription.getApprovedBy());
        response.setApprovedAt(map(subscription.getApprovedAt()));
        response.setRejectionReason(subscription.getRejectionReason());
        response.setRejectedBy(subscription.getRejectedBy());
        response.setRejectedAt(map(subscription.getRejectedAt()));
        response.setCardReceiptDate(subscription.getCardReceiptDate());
        response.setCreatedAt(map(subscription.getCreatedAt()));
        response.setCreatedBy(subscription.getCreatedBy());
        response.setUpdatedAt(map(subscription.getUpdatedAt()));
        response.setUpdatedBy(subscription.getUpdatedBy());
        return response;
    }

    private InvoiceDetailResponse toInvoiceDetailResponse(InvoiceDetail detail) {
        if (detail == null || detail.getInvoice() == null) {
            return null;
        }

        return new InvoiceDetailResponse(
                detail.getInvoice().getInvoiceId(),
                detail.getInvoice().getInvoiceNo(),
                detail.getInvoice().getCustomerId(),
                detail.getInvoice().getParkingSessionId(),
                detail.getInvoice().getSubscriptionId(),
                detail.getInvoice().getLostCardReportId(),
                detail.getInvoice().getAmount(),
                detail.getInvoice().getDiscountAmount(),
                detail.getInvoice().getFinalAmount(),
                detail.getInvoice().getStatus(),
                map(detail.getInvoice().getIssuedAt()),
                map(detail.getInvoice().getPaidAt()),
                map(detail.getInvoice().getCreatedAt()),
                detail.getInvoice().getCreatedBy(),
                map(detail.getInvoice().getUpdatedAt()),
                detail.getInvoice().getUpdatedBy(),
                toPaymentResponses(detail.getPayments())
        );
    }

    private List<PaymentResponse> toPaymentResponses(List<Payment> payments) {
        if (payments == null) {
            return List.of();
        }

        return payments.stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getInvoiceId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getTransactionRef(),
                payment.getStatus(),
                map(payment.getPaidAt()),
                payment.getReceivedBy(),
                payment.getNote()
        );
    }
}
