package com.ban.vehicle_management.domain.accesscontrol.lostcardreport.policy;

import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

public class LostCardReportPolicy {

    private static final int REPORTER_NAME_MIN_LENGTH = 2;
    private static final int REPORTER_NAME_MAX_LENGTH = 150;
    private static final int REGISTRATION_LICENSE_MIN_LENGTH = 5;
    private static final int REGISTRATION_LICENSE_MAX_LENGTH = 50;
    private static final Pattern REPORTER_NAME_PATTERN = Pattern.compile(
            "^[\\p{L}\\p{M}]+(?:[ '\\-\\u2019][\\p{L}\\p{M}]+)*$"
    );
    private static final Pattern VIETNAM_PHONE_PATTERN = Pattern.compile(
            "^(?:0[35789][0-9]{8}|\\+84[35789][0-9]{8})$"
    );
    private static final Pattern IDENTIFY_CARD_PATTERN = Pattern.compile("^[0-9]{9,12}$");
    private static final Pattern REGISTRATION_LICENSE_PATTERN = Pattern.compile(
            "^[\\p{L}\\p{N} ./-]+$"
    );

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
        report.setReporterName(normalizeReporterName(report.getReporterName()));
        report.setReporterPhone(normalizeReporterPhone(report.getReporterPhone()));
        report.setIdentifyCard(normalizeIdentifyCard(report.getIdentifyCard()));
        report.setRegistrationLicense(normalizeRegistrationLicense(report.getRegistrationLicense()));
        boolean hasIdentifyCard = report.getIdentifyCard() != null && !report.getIdentifyCard().isBlank();
        boolean hasRegistrationLicense = report.getRegistrationLicense() != null && !report.getRegistrationLicense().isBlank();

        if (!hasIdentifyCard && !hasRegistrationLicense) {
            throw new BadRequestException("Vui lòng nhập CCCD/CMND hoặc giấy đăng ký xe.");
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
            throw new BadRequestException("Thời gian mất thẻ không được ở tương lai.");
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

    private String normalizeReporterName(String reporterName) {
        String normalizedName = TextValidationUtils.normalizeNullableText(
                reporterName,
                "reporterName",
                REPORTER_NAME_MAX_LENGTH
        );
        if (normalizedName == null) {
            throw new BadRequestException("Vui lòng nhập người báo mất.");
        }
        if (normalizedName.length() < REPORTER_NAME_MIN_LENGTH) {
            throw new BadRequestException("Tên người báo mất phải có từ 2 đến 150 ký tự.");
        }
        if (!REPORTER_NAME_PATTERN.matcher(normalizedName).matches()) {
            throw new BadRequestException(
                    "Tên người báo mất chỉ được gồm chữ cái, khoảng trắng, dấu nháy đơn hoặc dấu gạch nối."
            );
        }
        return normalizedName;
    }

    private String normalizeReporterPhone(String reporterPhone) {
        String normalizedPhone = TextValidationUtils.normalizeNullableText(
                reporterPhone,
                "reporterPhone",
                20
        );
        if (normalizedPhone == null) {
            throw new BadRequestException("Vui lòng nhập số điện thoại người báo mất.");
        }
        if (!VIETNAM_PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new BadRequestException(
                    "Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx."
            );
        }
        return normalizedPhone;
    }

    private String normalizeIdentifyCard(String identifyCard) {
        String normalizedIdentifyCard = TextValidationUtils.normalizeNullableText(
                identifyCard,
                "identifyCard",
                12
        );
        if (normalizedIdentifyCard != null && !IDENTIFY_CARD_PATTERN.matcher(normalizedIdentifyCard).matches()) {
            throw new BadRequestException("CCCD/CMND phải có từ 9 đến 12 chữ số.");
        }
        return normalizedIdentifyCard;
    }

    private String normalizeRegistrationLicense(String registrationLicense) {
        String normalizedRegistrationLicense = TextValidationUtils.normalizeNullableText(
                registrationLicense,
                "registrationLicense",
                REGISTRATION_LICENSE_MAX_LENGTH
        );
        if (normalizedRegistrationLicense == null) {
            return null;
        }
        if (normalizedRegistrationLicense.length() < REGISTRATION_LICENSE_MIN_LENGTH
                || !REGISTRATION_LICENSE_PATTERN.matcher(normalizedRegistrationLicense).matches()) {
            throw new BadRequestException(
                    "Giấy đăng ký xe phải có từ 5 đến 50 ký tự và chỉ gồm chữ, số, khoảng trắng, dấu chấm, gạch nối hoặc dấu gạch chéo."
            );
        }
        return normalizedRegistrationLicense;
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

