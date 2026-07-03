package com.ban.vehicle_management.domain.accesscontrol.lostcardreport.policy;

import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class LostCardReportPolicy {

    public void initializeNewReport(LostCardReport report) {
        requireReport(report);

        if (report.getStatus() == null) {
            report.setStatus(LostCardReportStatus.OPEN);
        }
        if (report.getTicketPrice() == null) {
            report.setTicketPrice(BigDecimal.ZERO);
        }
        if (report.getLostCardFee() == null) {
            report.setLostCardFee(BigDecimal.ZERO);
        }

        validateState(report);
    }

    public void resolve(LostCardReport report, UUID resolvedBy, Instant resolvedAt) {
        requireStatus(report, LostCardReportStatus.OPEN);
        requireField(resolvedBy, "resolvedBy");
        requireField(resolvedAt, "resolvedAt");

        if (resolvedAt.isBefore(report.getNotificationTime())) {
            throw new BadRequestException("resolvedAt must not be before notificationTime");
        }

        report.setStatus(LostCardReportStatus.RESOLVED);
        report.setResolvedBy(resolvedBy);
        report.setResolvedAt(resolvedAt);
        validateState(report);
    }

    public void cancel(LostCardReport report, UUID cancelledBy, Instant cancelledAt, String cancelReason) {
        requireStatus(report, LostCardReportStatus.OPEN);
        requireField(cancelledBy, "cancelledBy");
        requireField(cancelledAt, "cancelledAt");

        if (cancelledAt.isBefore(report.getNotificationTime())) {
            throw new BadRequestException("cancelledAt must not be before notificationTime");
        }

        report.setStatus(LostCardReportStatus.CANCELLED);
        report.setCancelledBy(cancelledBy);
        report.setCancelledAt(cancelledAt);
        report.setCancelReason(TextValidationUtils.normalizeRequiredText(cancelReason, "cancelReason", 500));
        report.setResolvedBy(null);
        report.setResolvedAt(null);
        validateState(report);
    }

    public void validateState(LostCardReport report) {
        requireReport(report);
        requireField(report.getCardId(), "cardId");
        requireField(report.getNotificationTime(), "notificationTime");
        requireField(report.getTimeOfLost(), "timeOfLost");
        requireField(report.getStatus(), "status");
        requireField(report.getContext(), "context");
        report.setReporterName(TextValidationUtils.normalizeRequiredText(report.getReporterName(), "reporterName", 150));
        report.setReporterPhone(TextValidationUtils.normalizePhoneNumber(report.getReporterPhone(), "reporterPhone", 20));
        report.setIdentifyCard(TextValidationUtils.normalizeAlphaNumeric(report.getIdentifyCard(), "identifyCard", 20));
        report.setRegistrationLicense(TextValidationUtils.normalizeNullableText(report.getRegistrationLicense(), "registrationLicense", 50));
        boolean hasIdentifyCard = report.getIdentifyCard() != null && !report.getIdentifyCard().isBlank();
        boolean hasRegistrationLicense = report.getRegistrationLicense() != null && !report.getRegistrationLicense().isBlank();

        if (!hasIdentifyCard && !hasRegistrationLicense) {
            throw new BadRequestException("identifyCard or registrationLicense must be provided");
        }
        report.setNote(TextValidationUtils.normalizeNullableText(report.getNote(), "note", 500));

        BigDecimal ticketPrice = report.getTicketPrice() == null ? BigDecimal.ZERO : report.getTicketPrice();
        BigDecimal lostCardFee = report.getLostCardFee() == null ? BigDecimal.ZERO : report.getLostCardFee();

        if (ticketPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("ticketPrice must not be negative");
        }
        if (lostCardFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("lostCardFee must not be negative");
        }
        if (report.getNotificationTime().isBefore(report.getTimeOfLost())) {
            throw new BadRequestException("notificationTime must not be before timeOfLost");
        }

        boolean hasResolutionMetadata = report.getResolvedBy() != null || report.getResolvedAt() != null;
        boolean hasFullResolutionMetadata = report.getResolvedBy() != null && report.getResolvedAt() != null;

        if (report.getStatus() == LostCardReportStatus.RESOLVED) {
            if (!hasFullResolutionMetadata) {
                throw new BadRequestException("Resolved report must have resolvedBy and resolvedAt");
            }
            return;
        }

        if (hasResolutionMetadata) {
            throw new BadRequestException("Only resolved report can keep resolvedBy and resolvedAt");
        }

        boolean hasCancelMetadata = report.getCancelledBy() != null
                || report.getCancelledAt() != null
                || (report.getCancelReason() != null && !report.getCancelReason().isBlank());

        if (report.getStatus() == LostCardReportStatus.CANCELLED) {
            requireField(report.getCancelledBy(), "cancelledBy");
            requireField(report.getCancelledAt(), "cancelledAt");
            report.setCancelReason(TextValidationUtils.normalizeRequiredText(report.getCancelReason(), "cancelReason", 500));
            return;
        }

        if (hasCancelMetadata) {
            throw new BadRequestException("Only cancelled report can keep cancelledBy, cancelledAt, and cancelReason");
        }
    }

    private void requireStatus(LostCardReport report, LostCardReportStatus expectedStatus) {
        requireReport(report);
        if (report.getStatus() != expectedStatus) {
            throw new BadRequestException("Lost card report must be in " + expectedStatus + " status");
        }
    }

    private void requireReport(LostCardReport report) {
        requireField(report, "lostCardReport");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}

