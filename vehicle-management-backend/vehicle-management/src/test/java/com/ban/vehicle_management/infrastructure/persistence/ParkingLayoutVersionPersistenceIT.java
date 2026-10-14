package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkinglayout.model.command.SaveLayoutItemCommand;
import com.ban.vehicle_management.application.parking.parkinglayout.port.out.ParkingLayoutPortOut;
import com.ban.vehicle_management.application.parking.parkinglayout.usecase.ParkingLayoutUseCaseImpl;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.realtime.port.out.ParkingRealtimePublisherPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutElement;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers draft-layout persistence against real Hibernate behavior:
 * first insert with a null revision must succeed and seed revision 0,
 * a second update must increment it, and only a genuinely stale copy
 * may fail with an optimistic-locking conflict.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ParkingLayoutVersionPersistenceIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {

    @Autowired
    private ParkingLayoutPortOut parkingLayoutPortOut;

    @Autowired private ParkingSpacePortOut parkingSpacePortOut;
    @Autowired private ZonePortOut zonePortOut;
    @Autowired private ParkingLevelPortOut parkingLevelPortOut;
    @Autowired private ParkingLotPortOut parkingLotPortOut;
    @Autowired private ParkingSessionPortOut parkingSessionPortOut;
    @Autowired private ParkingSpaceAllocationPortOut allocationPortOut;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private static final UUID DEFAULT_ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000009001");

    private UUID zoneId;
    private UUID lotId;
    private UUID levelId;

    @BeforeEach
    void insertTopology() {
        lotId = UUID.randomUUID();
        levelId = UUID.randomUUID();
        zoneId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO parking.parking_lots(parking_lot_id, organization_id, code, name) VALUES (?, ?, 'LOT-DRAFT-IT', 'Lot Draft IT')",
                lotId, DEFAULT_ORGANIZATION_ID);
        jdbcTemplate.update(
                "INSERT INTO parking.parking_levels(parking_level_id, parking_lot_id, code, name, canvas_width, canvas_height) VALUES (?, ?, 'L1', 'Level 1', 1000, 700)",
                levelId, lotId);
        jdbcTemplate.update(
                "INSERT INTO parking.zones(zone_id, parking_lot_id, parking_level_id, code, name) VALUES (?, ?, ?, 'Z-DRAFT-IT', 'Zone Draft IT')",
                zoneId, lotId, levelId);
    }

    @Test
    void shouldInsertFirstDraftWithNullRevisionAndSeedZero() {
        ParkingLayoutVersion saved = parkingLayoutPortOut.saveLayoutVersion(newVersion(zoneId, 1L, null));

        assertEquals(0L, saved.getRevision());
        assertTrue(parkingLayoutPortOut.findDraftByZoneId(zoneId).isPresent());
    }

    @Test
    void shouldFailInsertWhenRevisionIsPresetInsteadOfSeeded() {
        // Guards the reported 409: pre-setting revision 0L routes save() to
        // merge(), which issues an UPDATE for a row that does not exist yet.
        assertThrows(OptimisticLockingFailureException.class,
                () -> parkingLayoutPortOut.saveLayoutVersion(newVersion(zoneId, 1L, 0L)));
    }

    @Test
    void shouldIncrementRevisionOnUpdateAndRejectOnlyStaleMerge() {
        ParkingLayoutVersion created = parkingLayoutPortOut.saveLayoutVersion(newVersion(zoneId, 1L, null));
        assertEquals(0L, created.getRevision());
        entityManager.clear();

        ParkingLayoutVersion fresh = parkingLayoutPortOut.findDraftByZoneId(zoneId).orElseThrow();
        ParkingLayoutVersion updated = parkingLayoutPortOut.saveLayoutVersion(fresh);
        assertEquals(1L, updated.getRevision());
        entityManager.clear();

        // The first copy still carries revision 0 while the row is at revision 1.
        assertThrows(OptimisticLockingFailureException.class,
                () -> parkingLayoutPortOut.saveLayoutVersion(created));
    }

    @Test
    void shouldPersistDraftSnapshotBeforeIncrementingRevision() {
        ParkingLayoutVersion draft = parkingLayoutPortOut.saveLayoutVersion(newVersion(zoneId, 1L, null));
        // The real create-draft and save-draft requests run in separate transactions.
        entityManager.clear();
        List<ParkingSpaceLayoutItem> items = java.util.stream.IntStream.rangeClosed(1, 28)
                .mapToObj(index -> layoutItem(draft.getLayoutVersionId(), index))
                .toList();
        ParkingLayoutElement element = new ParkingLayoutElement();
        element.setLayoutElementId(UUID.randomUUID());
        element.setLayoutVersionId(draft.getLayoutVersionId());
        element.setElementType("BACKGROUND");
        element.setGeometry(JsonNodeFactory.instance.objectNode()
                .put("x", 20).put("y", 30).put("width", 960).put("height", 620));
        element.setStyle(JsonNodeFactory.instance.objectNode().put("fill", "#f8fafc"));
        element.setLabel("Mặt bằng bãi");
        element.setDisplayOrder(0);

        assertEquals(28, parkingLayoutPortOut.saveLayoutItems(items).size());
        assertEquals(1, parkingLayoutPortOut.saveLayoutElements(List.of(element)).size());
        ParkingLayoutVersion saved = parkingLayoutPortOut.saveLayoutVersion(draft);

        assertEquals(1L, saved.getRevision());
        assertEquals(28, parkingLayoutPortOut.findLayoutItemsByVersionId(draft.getLayoutVersionId()).size());
        assertEquals(1, parkingLayoutPortOut.findLayoutElementsByVersionId(draft.getLayoutVersionId()).size());
    }

    @Test
    void shouldReplaceAnExistingDraftSnapshotUsingTheSameSpaceCodes() {
        ParkingLayoutVersion draft = parkingLayoutPortOut.saveLayoutVersion(newVersion(zoneId, 1L, null));
        entityManager.clear();

        parkingLayoutPortOut.saveLayoutItems(List.of(layoutItem(draft.getLayoutVersionId(), 1)));
        ParkingLayoutElement firstElement = layoutElement(draft.getLayoutVersionId(), "Mặt bằng lần 1");
        parkingLayoutPortOut.saveLayoutElements(List.of(firstElement));
        ParkingLayoutVersion firstSaved = parkingLayoutPortOut.saveLayoutVersion(draft);
        assertEquals(1L, firstSaved.getRevision());
        entityManager.clear();

        ParkingLayoutVersion fresh = parkingLayoutPortOut.findDraftByZoneId(zoneId).orElseThrow();
        parkingLayoutPortOut.deleteLayoutItemsByVersionId(fresh.getLayoutVersionId());
        parkingLayoutPortOut.saveLayoutItems(List.of(layoutItem(fresh.getLayoutVersionId(), 1)));
        parkingLayoutPortOut.deleteLayoutElementsByVersionId(fresh.getLayoutVersionId());
        parkingLayoutPortOut.saveLayoutElements(List.of(layoutElement(fresh.getLayoutVersionId(), "Mặt bằng lần 2")));
        ParkingLayoutVersion secondSaved = parkingLayoutPortOut.saveLayoutVersion(fresh);

        assertEquals(2L, secondSaved.getRevision());
        assertEquals(1, parkingLayoutPortOut.findLayoutItemsByVersionId(fresh.getLayoutVersionId()).size());
        assertEquals("Mặt bằng lần 2",
                parkingLayoutPortOut.findLayoutElementsByVersionId(fresh.getLayoutVersionId()).getFirst().getLabel());
    }

    @Test
    void shouldSaveTwiceThenPublishTheDraftAtomically() {
        OrganizationAccessGuard accessGuard = mock(OrganizationAccessGuard.class);
        AuditLogPortOut auditLogPortOut = mock(AuditLogPortOut.class);
        CurrentAccountPortIn currentAccountPortIn = mock(CurrentAccountPortIn.class);
        ParkingRealtimePublisherPortOut realtimePublisher = mock(ParkingRealtimePublisherPortOut.class);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        ParkingLayoutUseCaseImpl useCase = new ParkingLayoutUseCaseImpl(
                parkingLayoutPortOut,
                parkingSpacePortOut,
                zonePortOut,
                parkingLevelPortOut,
                parkingLotPortOut,
                parkingSessionPortOut,
                allocationPortOut,
                accessGuard,
                auditLogPortOut,
                currentAccountPortIn,
                realtimePublisher
        );
        List<SaveLayoutItemCommand> commands = List.of(
                layoutCommand("A-01", 50),
                layoutCommand("A-02", 120)
        );

        ParkingLayoutVersion draft = useCase.createDraftLayout(zoneId).version();
        var firstSave = useCase.saveLayout(zoneId, draft.getRevision(), commands, List.of());
        var secondSave = useCase.saveLayout(zoneId, firstSave.version().getRevision(), commands, List.of());
        var preview = useCase.previewPublish(zoneId, secondSave.version().getRevision());
        var published = useCase.publishLayout(zoneId, secondSave.version().getRevision());
        var editableDraft = useCase.createDraftLayout(zoneId);

        assertEquals(2, preview.newSpaces());
        assertEquals(2, preview.capacityAfterPublish());
        assertEquals(LayoutVersionStatus.PUBLISHED, published.version().getStatus());
        assertTrue(published.items().stream().allMatch(item -> item.parkingSpaceId() != null));
        assertEquals(LayoutVersionStatus.DRAFT, editableDraft.version().getStatus());
        assertEquals(2L, editableDraft.version().getVersion());
        assertEquals(2, editableDraft.items().size());
        assertTrue(editableDraft.items().stream().allMatch(item -> item.parkingSpaceId() != null));
        assertTrue(editableDraft.items().stream().noneMatch(editable -> published.items().stream()
                .anyMatch(live -> live.layoutItemId().equals(editable.layoutItemId()))));
        useCase.discardDraftLayout(zoneId, editableDraft.version().getRevision());
        assertTrue(parkingLayoutPortOut.findDraftByZoneId(zoneId).isEmpty());
        assertTrue(parkingLayoutPortOut.findPublishedByZoneId(zoneId).isPresent());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM parking.parking_spaces WHERE zone_id = ? AND lifecycle_status = 'ACTIVE'",
                Integer.class,
                zoneId));
        assertEquals("SPACE", jdbcTemplate.queryForObject(
                "SELECT tracking_mode FROM parking.zones WHERE zone_id = ?",
                String.class,
                zoneId));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT capacity FROM parking.zones WHERE zone_id = ?",
                Integer.class,
                zoneId));
    }

    private ParkingSpaceLayoutItem layoutItem(UUID layoutVersionId, int index) {
        ParkingSpaceLayoutItem item = new ParkingSpaceLayoutItem();
        item.setLayoutItemId(UUID.randomUUID());
        item.setLayoutVersionId(layoutVersionId);
        item.setCode("A-" + String.format("%02d", index));
        item.setX(BigDecimal.valueOf(50L + (long) (index - 1) * 62L));
        item.setY(BigDecimal.valueOf(70));
        item.setWidth(BigDecimal.valueOf(50));
        item.setHeight(BigDecimal.valueOf(100));
        item.setRotation(BigDecimal.ZERO);
        return item;
    }

    private ParkingLayoutElement layoutElement(UUID layoutVersionId, String label) {
        ParkingLayoutElement element = new ParkingLayoutElement();
        element.setLayoutElementId(UUID.randomUUID());
        element.setLayoutVersionId(layoutVersionId);
        element.setElementType("BACKGROUND");
        element.setGeometry(JsonNodeFactory.instance.objectNode()
                .put("x", 20).put("y", 30).put("width", 960).put("height", 620));
        element.setStyle(JsonNodeFactory.instance.objectNode().put("fill", "#f8fafc"));
        element.setLabel(label);
        element.setDisplayOrder(0);
        return element;
    }

    private SaveLayoutItemCommand layoutCommand(String code, long x) {
        return new SaveLayoutItemCommand(
                code,
                null,
                BigDecimal.valueOf(x),
                BigDecimal.valueOf(70),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(100),
                BigDecimal.ZERO
        );
    }

    private ParkingLayoutVersion newVersion(UUID zoneId, long version, Long revision) {
        ParkingLayoutVersion layoutVersion = new ParkingLayoutVersion();
        layoutVersion.setLayoutVersionId(UUID.randomUUID());
        layoutVersion.setZoneId(zoneId);
        layoutVersion.setVersion(version);
        layoutVersion.setStatus(LayoutVersionStatus.DRAFT);
        layoutVersion.setRevision(revision);
        return layoutVersion;
    }
}
