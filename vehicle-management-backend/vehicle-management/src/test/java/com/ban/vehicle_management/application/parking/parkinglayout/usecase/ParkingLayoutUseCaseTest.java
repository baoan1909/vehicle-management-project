package com.ban.vehicle_management.application.parking.parkinglayout.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkinglayout.model.command.SaveLayoutItemCommand;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.ZoneLayoutResult;
import com.ban.vehicle_management.application.parking.parkinglayout.port.out.ParkingLayoutPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutElement;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ParkingLayoutUseCaseTest {

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

    @BeforeEach
    void stubParkingLot() {
        when(parkingLotPortOut.findById(any())).thenAnswer(invocation ->
                Optional.of(lot((UUID) invocation.getArgument(0))));
    }

    @Test
    void shouldCreateFirstDraftWithNullRevisionForVersionSeeding() {
        UUID zoneId = UUID.randomUUID();
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.empty());
        when(layoutPortOut.findAllByZoneId(zoneId)).thenReturn(List.of());
        when(layoutPortOut.saveLayoutVersion(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ZoneLayoutResult result = useCase.createDraftLayout(zoneId);

        ArgumentCaptor<ParkingLayoutVersion> saved = ArgumentCaptor.forClass(ParkingLayoutVersion.class);
        verify(layoutPortOut).saveLayoutVersion(saved.capture());
        assertNull(saved.getValue().getRevision());
        assertEquals(1L, saved.getValue().getVersion());
        assertEquals(LayoutVersionStatus.DRAFT, saved.getValue().getStatus());
    }

    @Test
    void shouldReturnExistingDraftOnSecondCreateWithoutInsert() {
        UUID zoneId = UUID.randomUUID();
        ParkingLayoutVersion draft = draft(zoneId, 1L, 0L);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.of(draft));
        when(layoutPortOut.findLayoutItemsByVersionId(draft.getLayoutVersionId())).thenReturn(List.of());
        when(layoutPortOut.findLayoutElementsByVersionId(draft.getLayoutVersionId())).thenReturn(List.of());

        ZoneLayoutResult result = useCase.createDraftLayout(zoneId);

        assertEquals(draft.getLayoutVersionId(), result.version().getLayoutVersionId());
        assertEquals(0L, result.version().getRevision());
        verify(layoutPortOut, never()).saveLayoutVersion(any());
    }

    @Test
    void shouldCreateEditableDraftByCloningThePublishedLayout() {
        UUID zoneId = UUID.randomUUID();
        ParkingLayoutVersion published = draft(zoneId, 3L, 2L);
        published.setStatus(LayoutVersionStatus.PUBLISHED);
        ParkingSpaceLayoutItem publishedItem = layoutItem(published.getLayoutVersionId(), "A-01", 12, 18);
        publishedItem.setParkingSpaceId(UUID.randomUUID());
        ParkingLayoutElement publishedElement = new ParkingLayoutElement();
        publishedElement.setLayoutElementId(UUID.randomUUID());
        publishedElement.setLayoutVersionId(published.getLayoutVersionId());
        publishedElement.setElementType("WALL");
        publishedElement.setDisplayOrder(0);

        Zone zone = zone(zoneId);
        zone.setLayoutVersion(3L);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.empty());
        when(layoutPortOut.findAllByZoneId(zoneId)).thenReturn(List.of(published));
        when(layoutPortOut.findPublishedByZoneId(zoneId)).thenReturn(Optional.of(published));
        when(layoutPortOut.findLayoutItemsByVersionId(published.getLayoutVersionId()))
                .thenReturn(List.of(publishedItem));
        when(layoutPortOut.findLayoutElementsByVersionId(published.getLayoutVersionId()))
                .thenReturn(List.of(publishedElement));
        when(layoutPortOut.saveLayoutVersion(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(layoutPortOut.saveLayoutItems(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(layoutPortOut.saveLayoutElements(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ZoneLayoutResult result = useCase.createDraftLayout(zoneId);

        assertEquals(LayoutVersionStatus.DRAFT, result.version().getStatus());
        assertEquals(4L, result.version().getVersion());
        assertEquals(1, result.items().size());
        assertEquals(1, result.elements().size());
        assertEquals(publishedItem.getParkingSpaceId(), result.items().getFirst().parkingSpaceId());
        assertNotEquals(publishedItem.getLayoutItemId(), result.items().getFirst().layoutItemId());
        assertNotEquals(publishedElement.getLayoutElementId(), result.elements().getFirst().layoutElementId());
        assertEquals(result.version().getLayoutVersionId(), result.items().getFirst().layoutVersionId());
    }

    @Test
    void shouldDiscardOnlyTheExpectedDraftRevision() {
        UUID zoneId = UUID.randomUUID();
        ParkingLayoutVersion draft = draft(zoneId, 4L, 3L);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.of(draft));
        when(layoutPortOut.saveLayoutVersion(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.discardDraftLayout(zoneId, 3L);

        assertEquals(LayoutVersionStatus.ARCHIVED, draft.getStatus());
        verify(layoutPortOut).saveLayoutVersion(draft);
        verify(auditLogPortOut).save(any());
    }

    @Test
    void shouldSaveDraftWithoutMutatingLiveSpacesOrCapacity() {
        UUID zoneId = UUID.randomUUID();
        Zone zone = zone(zoneId);
        ParkingLayoutVersion draft = draft(zoneId, 1L, 0L);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(parkingLevelPortOut.findById(zone.getParkingLevelId()))
                .thenReturn(Optional.of(level(zone.getParkingLotId())));
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE"))
                .thenReturn(List.of());
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.of(draft));
        when(layoutPortOut.saveLayoutVersion(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(layoutPortOut.saveLayoutItems(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());

        ZoneLayoutResult result = useCase.saveLayout(zoneId, 0L,
                List.of(item("A-01", 0, 0), item("A-02", 20, 0)), List.of());

        assertEquals(2, result.items().size());
        assertEquals(LayoutVersionStatus.DRAFT, result.version().getStatus());
        verify(parkingSpacePortOut, never()).saveAll(any());
        verify(parkingSpacePortOut, never()).save(any(ParkingSpace.class));
        verify(zonePortOut, never()).save(any(Zone.class));
    }

    @Test
    void shouldPreserveElementsWhenFieldIsOmitted() {
        UUID zoneId = UUID.randomUUID();
        Zone zone = zone(zoneId);
        ParkingLayoutVersion draft = draft(zoneId, 1L, 0L);
        ParkingLayoutElement element = new ParkingLayoutElement();
        element.setLayoutElementId(UUID.randomUUID());
        element.setLayoutVersionId(draft.getLayoutVersionId());
        element.setElementType("WALL");
        element.setGeometry(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        element.setDisplayOrder(0);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(parkingLevelPortOut.findById(zone.getParkingLevelId()))
                .thenReturn(Optional.of(level(zone.getParkingLotId())));
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE"))
                .thenReturn(List.of());
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.of(draft));
        when(layoutPortOut.saveLayoutItems(any())).thenReturn(List.of());
        when(layoutPortOut.findLayoutElementsByVersionId(draft.getLayoutVersionId()))
                .thenReturn(List.of(element));
        when(layoutPortOut.saveLayoutVersion(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());

        ZoneLayoutResult result = useCase.saveLayout(zoneId, 0L, List.of(), null);

        assertEquals(1, result.elements().size());
        verify(layoutPortOut, never()).deleteLayoutElementsByVersionId(any());
    }
    @Test
    void shouldRejectSaveOnStaleDraftRevision() {
        UUID zoneId = UUID.randomUUID();
        ParkingLayoutVersion draft = draft(zoneId, 1L, 2L);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.of(draft));

        assertThrows(ConflictException.class,
                () -> useCase.saveLayout(zoneId, 1L, List.of(item("A-01", 0, 0)), List.of()));
        verify(layoutPortOut, never()).saveLayoutItems(any());
    }

    @Test
    void shouldPublishDraftAndSyncCapacities() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        Zone zone = zone(zoneId);
        zone.setParkingLotId(lotId);
        zone.setTrackingMode(TrackingMode.CAPACITY);
        zone.setCapacity(1000);
        ParkingSpace first = space(zoneId, "A-01");
        ParkingSpace second = space(zoneId, "A-02");
        ParkingLayoutVersion draft = draft(zoneId, 1L, 1L);
        ParkingSpaceLayoutItem firstItem = layoutItem(draft.getLayoutVersionId(), "A-01", 0, 0);
        ParkingSpaceLayoutItem secondItem = layoutItem(draft.getLayoutVersionId(), "A-02", 20, 0);

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.of(draft));
        when(layoutPortOut.findLayoutItemsByVersionId(draft.getLayoutVersionId()))
                .thenReturn(List.of(firstItem, secondItem));
        when(layoutPortOut.findLayoutElementsByVersionId(draft.getLayoutVersionId()))
                .thenReturn(List.of());
        when(parkingLevelPortOut.findById(zone.getParkingLevelId()))
                .thenReturn(Optional.of(level(lotId)));
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE"))
                .thenReturn(List.of(first, second));
        when(layoutPortOut.findPublishedByZoneId(zoneId)).thenReturn(Optional.empty());
        when(layoutPortOut.saveLayoutVersion(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(layoutPortOut.saveLayoutItems(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSpacePortOut.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(zonePortOut.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(zonePortOut.findAll(lotId, null, null, null)).thenReturn(List.of(zone));
        when(parkingLotPortOut.save(any(ParkingLot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());

        ZoneLayoutResult result = useCase.publishLayout(zoneId, 1L);

        assertEquals(LayoutVersionStatus.PUBLISHED, result.version().getStatus());
        assertEquals(TrackingMode.SPACE, zone.getTrackingMode());
        assertEquals(LayoutStatus.PUBLISHED, zone.getLayoutStatus());
        assertEquals(1L, zone.getLayoutVersion());
        assertEquals(2, zone.getCapacity());
        verify(parkingLotPortOut).save(any(ParkingLot.class));
        verify(auditLogPortOut).save(any());
    }

    @Test
    void shouldRejectPublishOfEmptyDraft() {
        UUID zoneId = UUID.randomUUID();
        ParkingLayoutVersion draft = draft(zoneId, 1L, 1L);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        when(layoutPortOut.findDraftByZoneId(zoneId)).thenReturn(Optional.of(draft));
        when(layoutPortOut.findLayoutItemsByVersionId(draft.getLayoutVersionId()))
                .thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> useCase.publishLayout(zoneId, 1L));
    }

    @Test
    void shouldDenyLayoutReadOutsideScope() {
        UUID zoneId = UUID.randomUUID();
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        doThrow(new AccessDeniedException("denied"))
                .when(organizationAccessGuard).ensureCanAccessParkingLot(any(ParkingLot.class));

        assertThrows(AccessDeniedException.class, () -> useCase.getDraftLayout(zoneId));
    }

    private Zone zone(UUID zoneId) {
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(UUID.randomUUID());
        zone.setParkingLevelId(UUID.randomUUID());
        zone.setCode("Z1");
        zone.setName("Zone 1");
        zone.setCapacity(0);
        zone.setStatus(ZoneStatus.ACTIVE);
        zone.setTrackingMode(TrackingMode.SPACE);
        zone.setLayoutStatus(LayoutStatus.NOT_CONFIGURED);
        zone.setLayoutVersion(0L);
        return zone;
    }

    private ParkingLevel level(UUID lotId) {
        ParkingLevel level = new ParkingLevel();
        level.setParkingLevelId(UUID.randomUUID());
        level.setParkingLotId(lotId);
        level.setCode("L1");
        level.setName("Level 1");
        level.setDisplayOrder(1);
        level.setCanvasWidth(BigDecimal.valueOf(100));
        level.setCanvasHeight(BigDecimal.valueOf(100));
        level.setStatus(ParkingLevelStatus.ACTIVE);
        return level;
    }

    private ParkingLot lot(UUID lotId) {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(UUID.randomUUID());
        lot.setTotalCapacity(0);
        return lot;
    }

    private ParkingSpace space(UUID zoneId, String code) {
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(UUID.randomUUID());
        space.setZoneId(zoneId);
        space.setCode(code);
        space.setStatus(ParkingSpaceStatus.AVAILABLE);
        space.setLifecycleStatus("ACTIVE");
        space.setStatusSource(ParkingSpaceStatusSource.MANUAL);
        space.setVersion(0L);
        space.setRotation(BigDecimal.ZERO);
        return space;
    }

    private SaveLayoutItemCommand item(String code, int x, int y) {
        return new SaveLayoutItemCommand(code, null,
                BigDecimal.valueOf(x), BigDecimal.valueOf(y),
                BigDecimal.valueOf(10), BigDecimal.valueOf(5), BigDecimal.ZERO);
    }

    private ParkingLayoutVersion draft(UUID zoneId, long version, long revision) {
        ParkingLayoutVersion draft = new ParkingLayoutVersion();
        draft.setLayoutVersionId(UUID.randomUUID());
        draft.setZoneId(zoneId);
        draft.setVersion(version);
        draft.setRevision(revision);
        draft.setStatus(LayoutVersionStatus.DRAFT);
        return draft;
    }

    private ParkingSpaceLayoutItem layoutItem(UUID versionId, String code, int x, int y) {
        ParkingSpaceLayoutItem item = new ParkingSpaceLayoutItem();
        item.setLayoutItemId(UUID.randomUUID());
        item.setLayoutVersionId(versionId);
        item.setCode(code);
        item.setX(BigDecimal.valueOf(x));
        item.setY(BigDecimal.valueOf(y));
        item.setWidth(BigDecimal.valueOf(10));
        item.setHeight(BigDecimal.valueOf(5));
        item.setRotation(BigDecimal.ZERO);
        return item;
    }
}
