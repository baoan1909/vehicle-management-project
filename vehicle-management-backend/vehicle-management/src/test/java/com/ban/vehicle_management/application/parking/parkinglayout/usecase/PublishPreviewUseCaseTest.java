package com.ban.vehicle_management.application.parking.parkinglayout.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.PublishPreviewResult;
import com.ban.vehicle_management.application.parking.parkinglayout.port.out.ParkingLayoutPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishPreviewUseCaseTest {

    @Mock private ParkingLayoutPortOut layoutPortOut;
    @Mock private ParkingSpacePortOut parkingSpacePortOut;
    @Mock private ZonePortOut zonePortOut;
    @Mock private ParkingLevelPortOut parkingLevelPortOut;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private ParkingSessionPortOut parkingSessionPortOut;
    @Mock private ParkingSpaceAllocationPortOut allocationPortOut;
    @Mock private OrganizationAccessGuard organizationAccessGuard;
    @Mock private AuditLogPortOut auditLogPortOut;
    @Mock private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks private ParkingLayoutUseCaseImpl useCase;

    @Test
    void shouldPreviewNewUpdatedAndArchivedCountsWithoutMutating() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        Zone zone = zone(zoneId, lotId, levelId);
        ParkingLayoutVersion draft = draft(zoneId, 3L, 7L);
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.of(draft));
        when(layoutPortOut.findLayoutItemsByVersionId(draft.getLayoutVersionId())).thenReturn(List.of(
                item("A-01"),
                item("A-02"),
                item("B-01")));
        when(layoutPortOut.findLayoutElementsByVersionId(draft.getLayoutVersionId()))
                .thenReturn(List.of());
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE")).thenReturn(List.of(
                space("A-01"),
                space("A-09")));

        zone.setTrackingMode(TrackingMode.CAPACITY);
        zone.setCapacity(1000);

        PublishPreviewResult preview = useCase.previewPublish(zoneId, 7L);

        assertEquals(3L, preview.draftVersion());
        assertEquals(7L, preview.draftRevision());
        assertEquals(2, preview.newSpaces());
        assertEquals(1, preview.updatedSpaces());
        assertEquals(1, preview.archivedSpaces());
        assertEquals(3, preview.capacityAfterPublish());
        verify(layoutPortOut, never()).saveLayoutVersion(any());
        verify(layoutPortOut, never()).saveLayoutItems(any());
        verify(parkingSpacePortOut, never()).saveAll(any());
        verify(zonePortOut, never()).save(any(Zone.class));
    }

    @Test
    void shouldFailPreviewWhenDraftIsMissing() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        Zone zone = zone(zoneId, lotId, UUID.randomUUID());
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.previewPublish(zoneId, 0L));
    }

    @Test
    void shouldRejectEmptyDraftPreview() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        Zone zone = zone(zoneId, lotId, UUID.randomUUID());
        ParkingLayoutVersion draft = draft(zoneId, 1L, 0L);
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.of(draft));
        when(layoutPortOut.findLayoutItemsByVersionId(draft.getLayoutVersionId())).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> useCase.previewPublish(zoneId, 0L));
    }

    @Test
    void shouldRejectPreviewForStaleDraftRevision() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        Zone zone = zone(zoneId, lotId, UUID.randomUUID());
        ParkingLayoutVersion draft = draft(zoneId, 2L, 4L);
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.of(draft));

        assertThrows(ConflictException.class, () -> useCase.previewPublish(zoneId, 3L));
    }

    private Zone zone(UUID zoneId, UUID lotId, UUID levelId) {
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(lotId);
        zone.setParkingLevelId(levelId);
        zone.setTrackingMode(TrackingMode.SPACE);
        zone.setLayoutVersion(2L);
        return zone;
    }

    private ParkingLayoutVersion draft(UUID zoneId, Long version, Long revision) {
        ParkingLayoutVersion draft = new ParkingLayoutVersion();
        draft.setLayoutVersionId(UUID.randomUUID());
        draft.setZoneId(zoneId);
        draft.setVersion(version);
        draft.setRevision(revision);
        draft.setStatus(LayoutVersionStatus.DRAFT);
        return draft;
    }

    private ParkingSpaceLayoutItem item(String code) {
        ParkingSpaceLayoutItem item = new ParkingSpaceLayoutItem();
        item.setLayoutItemId(UUID.randomUUID());
        item.setCode(code);
        item.setX(BigDecimal.ZERO);
        item.setY(BigDecimal.ZERO);
        item.setWidth(BigDecimal.valueOf(5));
        item.setHeight(BigDecimal.valueOf(5));
        item.setRotation(BigDecimal.ZERO);
        return item;
    }

    private ParkingSpace space(String code) {
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(UUID.randomUUID());
        space.setCode(code);
        return space;
    }
}
