package com.ban.vehicle_management.application.accesscontrol.lostcardreport.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.authorization.LostCardReportAccessGuard;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.out.LostCardReportPortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class LostCardReportPartnerScopeTest {

    @Mock private LostCardReportAccessGuard lostCardReportAccessGuard;
    @Mock private LostCardReportPortOut lostCardReportPortOut;
    @Mock private CardPortOut cardPortOut;
    @Mock private OrganizationAccessGuard organizationAccessGuard;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @InjectMocks private LostCardReportUseCaseImpl useCase;

    @Test
    void listUsesOnlyParkingLotsOwnedByPartner() {
        Set<UUID> ownLots = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(ownLots);

        useCase.getReportListItems(null, null, null, null, null, null, null, null, null, null);

        verify(lostCardReportPortOut).findListItems(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(ownLots));
    }

    @Test
    void summaryUsesOnlyParkingLotsOwnedByPartner() {
        Set<UUID> ownLots = Set.of(UUID.randomUUID());
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(ownLots);

        useCase.getSummary(null, null, null);

        verify(lostCardReportPortOut).countByStatus(LostCardReportStatus.OPEN, ownLots);
        verify(lostCardReportPortOut).countOpenByInvoiceStatus(InvoiceStatus.UNPAID, ownLots);
        verify(lostCardReportPortOut).countByStatusAndResolvedAtBetween(
                LostCardReportStatus.RESOLVED, null, null, ownLots);
        verify(lostCardReportPortOut).countDistinctCardsByCardStatus(CardStatus.LOST, ownLots);
    }

    @Test
    void selectedParkingLotMustBelongToPartner() {
        UUID ownLot = UUID.randomUUID();
        UUID otherLot = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(ownLot));

        assertThrows(AccessDeniedException.class,
                () -> useCase.getReportListItems(null, null, null, null, null, null, otherLot, null, null, null));
        assertThrows(AccessDeniedException.class, () -> useCase.getSummary(null, null, otherLot));
        verify(lostCardReportPortOut, never()).findListItems(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void selectedParkingLotNarrowsListAndSummaryToThatLot() {
        UUID selectedLot = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(selectedLot, UUID.randomUUID()));

        useCase.getReportListItems(null, null, null, null, null, null, selectedLot, null, null, null);
        useCase.getSummary(null, null, selectedLot);

        verify(lostCardReportPortOut).findListItems(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(Set.of(selectedLot)));
        verify(lostCardReportPortOut).countByStatus(LostCardReportStatus.OPEN, Set.of(selectedLot));
    }

    @Test
    void directReportIdFromAnotherPartnerIsDeniedBeforeDetailsAreLoaded() {
        UUID reportId = UUID.randomUUID();
        LostCardReport report = new LostCardReport();
        report.setParkingLotId(UUID.randomUUID());
        when(lostCardReportPortOut.findById(reportId)).thenReturn(Optional.of(report));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(UUID.randomUUID()));

        assertThrows(AccessDeniedException.class, () -> useCase.getReportById(reportId));
        verify(lostCardReportPortOut, never()).save(any());
    }

    @Test
    void directResolveOfAnotherPartnersReportIsDeniedBeforeMutation() {
        UUID reportId = UUID.randomUUID();
        LostCardReport report = new LostCardReport();
        report.setParkingLotId(UUID.randomUUID());
        when(lostCardReportPortOut.findById(reportId)).thenReturn(Optional.of(report));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(UUID.randomUUID()));

        assertThrows(AccessDeniedException.class, () -> useCase.resolveReport(reportId, null));
        verify(lostCardReportPortOut, never()).save(any());
    }

    @Test
    void historicalReportWithoutProvenParkingLotIsHiddenFromPartner() {
        UUID reportId = UUID.randomUUID();
        when(lostCardReportPortOut.findById(reportId)).thenReturn(Optional.of(new LostCardReport()));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(UUID.randomUUID()));

        assertThrows(AccessDeniedException.class, () -> useCase.getReportById(reportId));
    }
}
