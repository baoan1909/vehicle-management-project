package com.ban.vehicle_management.domain.accesscontrol.lostcardreport.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.shared.enumeration.LostCardReportStatus;
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

        lostCardReportPolicy.cancel(report);

        assertEquals(LostCardReportStatus.CANCELLED, report.getStatus());
        assertNull(report.getResolvedBy());
        assertNull(report.getResolvedAt());
    }

    private LostCardReport validReport(LostCardReportStatus status) {
        LostCardReport report = new LostCardReport();
        report.setLostCardReportId(UUID.randomUUID());
        report.setCardId(UUID.randomUUID());
        report.setNotificationTime(Instant.parse("2026-05-15T11:00:00Z"));
        report.setTimeOfLost(Instant.parse("2026-05-15T10:00:00Z"));
        report.setTicketPrice(new BigDecimal("10000"));
        report.setLostCardFee(new BigDecimal("50000"));
        report.setStatus(status);
        return report;
    }
}

