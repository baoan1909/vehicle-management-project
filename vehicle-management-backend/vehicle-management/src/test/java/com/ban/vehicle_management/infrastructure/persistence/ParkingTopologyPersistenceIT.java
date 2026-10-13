package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.gate.usecase.GateUseCaseImpl;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.lane.usecase.LaneUsecaseImpl;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.parking.zone.usecase.ZoneUseCaseImpl;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves zone/gate/lane topology persistence against PostgreSQL:
 * create -&gt; read -&gt; update -&gt; status transitions -&gt; soft close,
 * with parent scope and lot isolation verified at the row level.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ParkingTopologyPersistenceIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {

    private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000009001");

    @Autowired
    private ZonePortOut zonePortOut;

    @Autowired
    private GatePortOut gatePortOut;

    @Autowired
    private LanePortOut lanePortOut;

    @Autowired
    private ParkingLotPortOut parkingLotPortOut;

    @Autowired
    private ParkingLevelPortOut parkingLevelPortOut;

    @Autowired
    private ParkingSpacePortOut parkingSpacePortOut;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID lotA;
    private UUID lotB;
    private UUID levelA1;
    private UUID levelA2;
    private UUID levelB1;
    private UUID vehicleTypeId;

    private ZoneUseCaseImpl zoneUseCase;
    private GateUseCaseImpl gateUseCase;
    private LaneUsecaseImpl laneUseCase;

    @BeforeEach
    void insertTopology() {
        lotA = UUID.randomUUID();
        lotB = UUID.randomUUID();
        levelA1 = UUID.randomUUID();
        levelA2 = UUID.randomUUID();
        levelB1 = UUID.randomUUID();
        vehicleTypeId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO parking.parking_lots(parking_lot_id, organization_id, code, name) VALUES (?, ?, 'LOT-TOPO-A', 'Lot Topology A')",
                lotA, ORGANIZATION_ID);
        jdbcTemplate.update(
                "INSERT INTO parking.parking_lots(parking_lot_id, organization_id, code, name) VALUES (?, ?, 'LOT-TOPO-B', 'Lot Topology B')",
                lotB, ORGANIZATION_ID);
        jdbcTemplate.update(
                "INSERT INTO parking.parking_levels(parking_level_id, parking_lot_id, code, name, canvas_width, canvas_height) VALUES (?, ?, 'L1', 'Level 1', 1000, 700)",
                levelA1, lotA);
        jdbcTemplate.update(
                "INSERT INTO parking.parking_levels(parking_level_id, parking_lot_id, code, name, canvas_width, canvas_height) VALUES (?, ?, 'L2', 'Level 2', 1000, 700)",
                levelA2, lotA);
        jdbcTemplate.update(
                "INSERT INTO parking.parking_levels(parking_level_id, parking_lot_id, code, name, canvas_width, canvas_height) VALUES (?, ?, 'L1', 'Level 1', 1000, 700)",
                levelB1, lotB);
        jdbcTemplate.update(
                "INSERT INTO catalog.vehicle_types(vehicle_type_id, organization_id, code, name, is_active) VALUES (?, ?, 'CAR-IT', 'O to IT', true)",
                vehicleTypeId, ORGANIZATION_ID);

        OrganizationAccessGuard accessGuard = mock(OrganizationAccessGuard.class);
        NotificationPortIn notificationPortIn = mock(NotificationPortIn.class);
        zoneUseCase = new ZoneUseCaseImpl(zonePortOut, parkingLotPortOut, parkingLevelPortOut, parkingSpacePortOut, accessGuard);
        gateUseCase = new GateUseCaseImpl(gatePortOut, zonePortOut, parkingLotPortOut, accessGuard);
        laneUseCase = new LaneUsecaseImpl(lanePortOut, notificationPortIn, gatePortOut, zonePortOut, parkingLotPortOut, accessGuard);
    }

    @Test
    void shouldCreateAndReadZoneWithCorrectParentScope() {
        Zone request = newZone(levelA1, "Z1", "Zone 1", 50);

        Zone created = zoneUseCase.createZone(request);

        assertEquals(lotA, created.getParkingLotId());
        assertEquals(levelA1, created.getParkingLevelId());
        assertEquals("Z1", created.getCode());
        assertEquals(ZoneStatus.ACTIVE, created.getStatus());

        Zone reloaded = zoneUseCase.getZoneById(created.getZoneId());
        assertEquals(created.getZoneId(), reloaded.getZoneId());
        assertEquals(levelA1, reloaded.getParkingLevelId());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT parking_lot_id, parking_level_id, code FROM parking.zones WHERE zone_id = ?", created.getZoneId());
        assertEquals(lotA, row.get("parking_lot_id"));
        assertEquals(levelA1, row.get("parking_level_id"));
        assertEquals("Z1", row.get("code"));
    }

    @Test
    void shouldRejectZoneLevelFromAnotherParkingLot() {
        Zone request = newZone(levelB1, "ZX", "Zone X", 10);
        request.setParkingLotId(lotA);

        assertThrows(BadRequestException.class, () -> zoneUseCase.createZone(request));
    }

    @Test
    void shouldRejectDuplicateZoneCodeInSameLot() {
        zoneUseCase.createZone(newZone(levelA1, "DUP", "Zone Dup", 10));

        assertThrows(ConflictException.class, () -> zoneUseCase.createZone(newZone(levelA2, "dup", "Zone Dup 2", 5)));
    }

    @Test
    void shouldUpdateZoneLevelWithinSameLot() {
        Zone created = zoneUseCase.createZone(newZone(levelA1, "MV", "Zone Move", 20));

        Zone update = new Zone();
        update.setCode("MV");
        update.setName("Zone Moved");
        update.setParkingLevelId(levelA2);
        update.setVehicleTypeId(vehicleTypeId);
        update.setVehicleTypeIds(Set.of(vehicleTypeId));
        update.setCapacity(25);
        Zone updated = zoneUseCase.updateZone(created.getZoneId(), update);

        assertEquals(levelA2, updated.getParkingLevelId());
        assertEquals("Zone Moved", updated.getName());
        assertEquals(25, updated.getCapacity());
    }

    @Test
    void shouldTransitionZoneStatusAndSoftClose() {
        Zone created = zoneUseCase.createZone(newZone(levelA1, "ST", "Zone Status", 10));

        assertEquals(ZoneStatus.MAINTENANCE, zoneUseCase.markZoneMaintenance(created.getZoneId()).getStatus());
        assertEquals(ZoneStatus.ACTIVE, zoneUseCase.activateZone(created.getZoneId()).getStatus());

        Zone closed = zoneUseCase.closeZone(created.getZoneId());
        assertEquals(ZoneStatus.CLOSED, closed.getStatus());
        assertTrue(zonePortOut.findById(created.getZoneId()).isPresent(),
                "Soft close must keep the historical row");
    }

    @Test
    void shouldRejectClosingZoneWithActiveGate() {
        Zone zone = zoneUseCase.createZone(newZone(levelA1, "ZG", "Zone Gate", 10));
        gateUseCase.createGate(newGate(zone.getZoneId(), "G1", "Gate 1"));

        assertThrows(ConflictException.class, () -> zoneUseCase.closeZone(zone.getZoneId()));
    }

    @Test
    void shouldCreateGateAndEnforceCodeScopePerZone() {
        Zone zoneA = zoneUseCase.createZone(newZone(levelA1, "GA", "Zone GA", 10));
        Zone zoneB = zoneUseCase.createZone(newZone(levelA1, "GB", "Zone GB", 10));

        Gate created = gateUseCase.createGate(newGate(zoneA.getZoneId(), "g1", "Gate 1"));
        assertEquals("G1", created.getCode());
        assertEquals(GateStatus.ACTIVE, created.getStatus());

        assertThrows(ConflictException.class, () -> gateUseCase.createGate(newGate(zoneA.getZoneId(), "G1", "Gate Dup")));

        Gate otherZone = gateUseCase.createGate(newGate(zoneB.getZoneId(), "G1", "Gate Same Code"));
        assertEquals(zoneB.getZoneId(), otherZone.getZoneId());
    }

    @Test
    void shouldRejectClosingGateWithActiveLane() {
        Zone zone = zoneUseCase.createZone(newZone(levelA1, "GL", "Zone GL", 10));
        Gate gate = gateUseCase.createGate(newGate(zone.getZoneId(), "G9", "Gate 9"));
        Lane lane = laneUseCase.createLane(newLane(gate.getGateId(), "L1", "Lane 1", LaneDirection.IN));

        assertThrows(ConflictException.class, () -> gateUseCase.closeGate(gate.getGateId()));

        laneUseCase.closeLane(lane.getLaneId());
        assertEquals(GateStatus.CLOSED, gateUseCase.closeGate(gate.getGateId()).getStatus());
    }

    @Test
    void shouldCreateUpdateAndTransitionLane() {
        Zone zone = zoneUseCase.createZone(newZone(levelA1, "LN", "Zone Lane", 10));
        Gate gate = gateUseCase.createGate(newGate(zone.getZoneId(), "GL1", "Gate Lane"));

        Lane created = laneUseCase.createLane(newLane(gate.getGateId(), "l-in", "Lane In", LaneDirection.IN));
        assertEquals("L-IN", created.getCode());
        assertEquals(LaneDirection.IN, created.getDirection());
        assertEquals(LaneStatus.ACTIVE, created.getStatus());

        Lane update = new Lane();
        update.setCode("L-OUT");
        update.setName("Lane Out");
        update.setDirection(LaneDirection.OUT);
        Lane updated = laneUseCase.updateLane(created.getLaneId(), update);
        assertEquals(LaneDirection.OUT, updated.getDirection());

        assertThrows(ConflictException.class,
                () -> laneUseCase.createLane(newLane(gate.getGateId(), "L-OUT", "Lane Dup", LaneDirection.IN)));

        assertEquals(LaneStatus.MAINTENANCE, laneUseCase.markLaneMaintenance(created.getLaneId()).getStatus());
        assertEquals(LaneStatus.ACTIVE, laneUseCase.activateLane(created.getLaneId()).getStatus());
        assertEquals(LaneStatus.CLOSED, laneUseCase.closeLane(created.getLaneId()).getStatus());
        assertTrue(lanePortOut.findById(created.getLaneId()).isPresent(),
                "Soft close must keep the historical row");
    }

    @Test
    void databaseTriggerShouldRejectZoneLevelFromAnotherLot() {
        UUID zoneId = UUID.randomUUID();
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO parking.zones(zone_id, parking_lot_id, parking_level_id, code, name) VALUES (?, ?, ?, 'Z-TRG', 'Zone Trigger')",
                zoneId, lotA, levelB1));
    }

    private Zone newZone(UUID levelId, String code, String name, int capacity) {
        Zone zone = new Zone();
        zone.setParkingLotId(lotA);
        zone.setParkingLevelId(levelId);
        zone.setCode(code);
        zone.setName(name);
        zone.setVehicleTypeId(vehicleTypeId);
        zone.setVehicleTypeIds(Set.of(vehicleTypeId));
        zone.setCapacity(capacity);
        zone.setTrackingMode(TrackingMode.CAPACITY);
        return zone;
    }

    private Gate newGate(UUID zoneId, String code, String name) {
        Gate gate = new Gate();
        gate.setZoneId(zoneId);
        gate.setCode(code);
        gate.setName(name);
        return gate;
    }

    private Lane newLane(UUID gateId, String code, String name, LaneDirection direction) {
        Lane lane = new Lane();
        lane.setGateId(gateId);
        lane.setCode(code);
        lane.setName(name);
        lane.setDirection(direction);
        return lane;
    }
}
