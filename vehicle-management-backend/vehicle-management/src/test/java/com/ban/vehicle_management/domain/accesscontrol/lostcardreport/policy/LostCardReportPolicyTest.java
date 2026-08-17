package com.ban.vehicle_management.domain.accesscontrol.lostcardreport.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LostCardReportPolicyTest {

    private final LostCardReportPolicy lostCardReportPolicy = new LostCardReportPolicy();

    @Test
    void shouldInitializeNewReportWithDefaults() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setStatus(null);
        report.setTicketPrice(null);
        report.setLostCardFee(null);

        lostCardReportPolicy.initializeNewReport(report);

        assertEquals(LostCardReportStatus.OPEN, report.getStatus());
        assertEquals(BigDecimal.ZERO, report.getTicketPrice());
        assertEquals(BigDecimal.ZERO, report.getLostCardFee());
    }

    @Test
    void shouldResolveOpenReport() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        UUID resolvedBy = UUID.randomUUID();
        Instant resolvedAt = Instant.parse("2026-05-15T12:00:00Z");

        lostCardReportPolicy.resolve(report, resolvedBy, resolvedAt);

        assertEquals(LostCardReportStatus.RESOLVED, report.getStatus());
        assertEquals(resolvedBy, report.getResolvedBy());
        assertEquals(resolvedAt, report.getResolvedAt());
    }

    @Test
    void shouldRejectNotificationBeforeLossTime() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setNotificationTime(report.getTimeOfLost().minusSeconds(60));

        assertThrows(BadRequestException.class, () -> lostCardReportPolicy.validateState(report));
    }

    @Test
    void shouldRejectNegativeFees() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setLostCardFee(new BigDecimal("-1"));

        assertThrows(BadRequestException.class, () -> lostCardReportPolicy.validateState(report));
    }

    @Test
    void shouldCancelOpenReportAndClearResolutionMetadata() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setResolvedBy(UUID.randomUUID());
        report.setResolvedAt(Instant.parse("2026-05-15T12:00:00Z"));
        UUID cancelledBy = UUID.randomUUID();
        Instant cancelledAt = Instant.parse("2026-05-15T12:00:00Z");

        lostCardReportPolicy.cancel(report, cancelledBy, cancelledAt, "Khach tim lai duoc the");

        assertEquals(LostCardReportStatus.CANCELLED, report.getStatus());
        assertEquals(cancelledBy, report.getCancelledBy());
        assertEquals(cancelledAt, report.getCancelledAt());
        assertEquals("Khach tim lai duoc the", report.getCancelReason());
        assertNull(report.getResolvedBy());
        assertNull(report.getResolvedAt());
    }

    @Test
    void shouldNormalizeLostCardReporterFields() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setReporterName("  Nguyen Van A  ");
        report.setReporterPhone(" 0901234567 ");
        report.setIdentifyCard(" 080112345678 ");
        report.setRegistrationLicense(" 51A-12345 ");
        report.setNote(" Mat the o cong vao ");

        lostCardReportPolicy.validateState(report);

        assertEquals("Nguyen Van A", report.getReporterName());
        assertEquals("0901234567", report.getReporterPhone());
        assertEquals("080112345678", report.getIdentifyCard());
        assertEquals("51A-12345", report.getRegistrationLicense());
        assertEquals("Mat the o cong vao", report.getNote());
    }

    @Test
    void shouldRejectReporterNameExceedingSchemaLength() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setReporterName("A".repeat(151));

        assertThrows(BadRequestException.class, () -> lostCardReportPolicy.validateState(report));
    }

    @Test
    void shouldRejectInvalidReporterPhoneFormat() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setReporterPhone("0901-234-567");

        assertThrows(BadRequestException.class, () -> lostCardReportPolicy.validateState(report));
    }

    @Test
    void shouldAcceptVietnamPhoneWithCountryCode() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setReporterPhone("+84901234567");

        lostCardReportPolicy.validateState(report);

        assertEquals("+84901234567", report.getReporterPhone());
    }

    @Test
    void shouldRejectInvalidReporterName() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setReporterName("Nguyen Van A 123");

        assertThrows(BadRequestException.class, () -> lostCardReportPolicy.validateState(report));
    }

    @Test
    void shouldRejectIdentifyCardOutsideAcceptedLength() {
        String[] invalidIdentifyCards = {"12345678", "1234567890123", "ABC123456"};

        for (String identifyCard : invalidIdentifyCards) {
            LostCardReport report = validReport(LostCardReportStatus.OPEN);
            report.setIdentifyCard(identifyCard);

            assertThrows(BadRequestException.class, () -> lostCardReportPolicy.validateState(report));
        }
    }

    @Test
    void shouldAcceptNineDigitIdentifyCard() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setIdentifyCard("123456789");

        lostCardReportPolicy.validateState(report);

        assertEquals("123456789", report.getIdentifyCard());
    }

    @Test
    void shouldRejectInvalidRegistrationLicense() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setIdentifyCard(null);
        report.setRegistrationLicense("A<1>");

        assertThrows(BadRequestException.class, () -> lostCardReportPolicy.validateState(report));
    }

    @Test
    void shouldRequireIdentifyCardOrRegistrationLicense() {
        LostCardReport report = validReport(LostCardReportStatus.OPEN);
        report.setIdentifyCard(null);
        report.setRegistrationLicense(" ");

        assertThrows(BadRequestException.class, () -> lostCardReportPolicy.validateState(report));
    }

    private LostCardReport validReport(LostCardReportStatus status) {
        LostCardReport report = new LostCardReport();
        report.setLostCardReportId(UUID.randomUUID());
        report.setCardId(UUID.randomUUID());
        report.setContext(LostCardReportContext.VISITOR_IN_PARKING);
        report.setNotificationTime(Instant.parse("2026-05-15T11:00:00Z"));
        report.setTimeOfLost(Instant.parse("2026-05-15T10:00:00Z"));
        report.setTicketPrice(new BigDecimal("10000"));
        report.setLostCardFee(new BigDecimal("50000"));
        report.setReporterName("Nguyen Van A");
        report.setReporterPhone("0901234567");
        report.setIdentifyCard("080112345678");
        report.setStatus(status);
        return report;
    }
}

