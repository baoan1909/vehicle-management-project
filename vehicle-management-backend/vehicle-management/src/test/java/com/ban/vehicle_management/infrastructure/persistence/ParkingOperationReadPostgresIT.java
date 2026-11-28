package com.ban.vehicle_management.infrastructure.persistence;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.entrypoint.controller.parking.ParkingOperationReadController;
import com.ban.vehicle_management.entrypoint.controller.parking.ParkingLevelController;
import com.ban.vehicle_management.entrypoint.controller.hardware.DeviceController;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ParkingOperationReadPostgresIT extends PostgresIntegrationTestSupport {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ParkingOperationReadController controller;
    @Autowired private ParkingLevelController adminController;
    @Autowired private DeviceController deviceController;
    @MockitoBean private CurrentAccountPortIn currentAccountPortIn;
    private UUID organizationId, lotId, levelId, zoneId, spaceId;

    @BeforeEach
    void createNewLot() {
        organizationId = UUID.randomUUID();
        lotId = UUID.randomUUID(); levelId = UUID.randomUUID(); zoneId = UUID.randomUUID(); spaceId = UUID.randomUUID();
        jdbc.update("insert into iam.organizations(organization_id,code,name,status) values (?,?,?,'ACTIVE')",
                organizationId, "ORG-" + organizationId, "Operation test organization");
        insertLot(lotId, organizationId);
        jdbc.update("insert into parking.parking_levels(parking_level_id,parking_lot_id,code,name,status) values (?,?,'L1','New level','ACTIVE')",levelId,lotId);
        jdbc.update("insert into parking.zones(zone_id,parking_lot_id,parking_level_id,code,name,capacity,status,tracking_mode) values (?,?,?,'Z1','New zone',1,'ACTIVE','SPACE')",zoneId,lotId,levelId);
        UUID gateId = UUID.randomUUID();
        jdbc.update("insert into parking.gates(gate_id,zone_id,code,name,status) values (?,?,'G1','New gate','ACTIVE')",gateId,zoneId);
        jdbc.update("insert into parking.lanes(lane_id,gate_id,code,name,direction,status) values (?,?,'IN','Entry','IN','ACTIVE')",UUID.randomUUID(),gateId);
        jdbc.update("insert into parking.lanes(lane_id,gate_id,code,name,direction,status) values (?,?,'OUT','Exit','OUT','ACTIVE')",UUID.randomUUID(),gateId);
        jdbc.update("insert into parking.parking_spaces(parking_space_id,zone_id,code,status,lifecycle_status) values (?,?,'NEW-01','AVAILABLE','ACTIVE')",spaceId,zoneId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operator", "", List.of()));
    }
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    private void insertLot(UUID id, UUID org) {
        jdbc.update("insert into parking.parking_lots(parking_lot_id,organization_id,code,name,total_capacity,status) values (?,?,?,'New operation lot',1,'SETUP')",id,org,"LOT-"+id);
    }
    private void actor(String role, boolean permitted) {
        String scope = role.equals("PARTNER_ADMIN") ? "PARKING_SCOPE_PARTNER" : "PARKING_SCOPE_ASSIGNED";
        var permissions = permitted ? Set.of(scope,"PARKING_SESSION_CHECK_IN_ALL","PARKING_SESSION_CHECK_OUT_ALL") : Set.of(scope,"PARKING_LOT_READ_ALL");
        var account = new CurrentAccountAccess(UUID.randomUUID(),"subject","operator","operator@example.test",UUID.randomUUID(),role,true,true,
                Set.of(organizationId),Set.of(lotId),AccountStatus.ACTIVE,EmployeeStatus.ACTIVE,null,null,false,ApprovalRequestStatus.APPROVED,permissions);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account);
        when(currentAccountPortIn.getCurrentAccount()).thenReturn(Optional.of(account));
        when(currentAccountPortIn.hasPermission(anyString())).thenAnswer(call -> account.getEffectivePermissionCodes().contains(call.getArgument(0)));
    }

    @ParameterizedTest
    @ValueSource(strings={"EMPLOYEE","PARKING_MANAGER","PARTNER_ADMIN"})
    void newlyCreatedLotIsReadableWithoutAdministrativePermission(String role) {
        actor(role,true);
        assertEquals(levelId,controller.levels(lotId).getData().getFirst().getParkingLevelId());
        assertEquals(zoneId,controller.zones(lotId,null).getData().getFirst().getZoneId());
        assertEquals("IN",controller.lanes(lotId,LaneDirection.IN).getData().getFirst().getCode());
        assertEquals("OUT",controller.lanes(lotId,LaneDirection.OUT).getData().getFirst().getCode());
        assertEquals(spaceId,controller.map(levelId).getData().spaces().getFirst().getParkingSpaceId());
        assertEquals(1,controller.summary(lotId).getData().getTotal());
        assertTrue(controller.allocatedSpaces(zoneId).getData().isEmpty());
        assertThrows(AccessDeniedException.class,()->adminController.activateLevel(levelId));
    }
    @ParameterizedTest
    @ValueSource(strings={"EMPLOYEE","PARKING_MANAGER","PARTNER_ADMIN"})
    void rejectsAnotherOrganizationAndUnassignedLot(String role) {
        actor(role,true);
        UUID anotherOrg=UUID.randomUUID(), anotherLot=UUID.randomUUID();
        jdbc.update("insert into iam.organizations(organization_id,code,name,status) values (?,?,?,'ACTIVE')",anotherOrg,"ORG-"+anotherOrg,"Other organization");
        insertLot(anotherLot,anotherOrg);
        assertThrows(AccessDeniedException.class,()->controller.levels(anotherLot));
        assertThrows(AccessDeniedException.class,()->controller.zones(anotherLot,null));
        assertThrows(AccessDeniedException.class,()->controller.lanes(anotherLot,LaneDirection.IN));
        assertThrows(AccessDeniedException.class,()->controller.summary(anotherLot));
        if (!role.equals("PARTNER_ADMIN")) {
            UUID unassignedLot = UUID.randomUUID();
            insertLot(unassignedLot, organizationId);
            assertThrows(AccessDeniedException.class,()->controller.levels(unassignedLot));
            assertThrows(AccessDeniedException.class,()->controller.lanes(unassignedLot,LaneDirection.IN));
        }

    }
    @Test void partnerSeesEmptyNewLotInOwnOrganization() {
        actor("PARTNER_ADMIN",true);
        UUID emptyLot=UUID.randomUUID(); insertLot(emptyLot,organizationId);
        assertTrue(controller.levels(emptyLot).getData().isEmpty());
        assertTrue(controller.lanes(emptyLot,LaneDirection.IN).getData().isEmpty());
        assertEquals(0,controller.summary(emptyLot).getData().getTotal());
    }
    @Test void revokedOperationPermissionDeniesControllerRead() {
        actor("EMPLOYEE",false);
        assertThrows(AccessDeniedException.class,()->controller.levels(lotId));
    }

    private void deviceReader(String role) {
        String scope = role.equals("PARTNER_ADMIN") ? "PARKING_SCOPE_PARTNER" : "PARKING_SCOPE_ASSIGNED";
        var account = new CurrentAccountAccess(UUID.randomUUID(),"subject","reader","reader@example.test",UUID.randomUUID(),role,true,true,
                Set.of(organizationId),Set.of(lotId),AccountStatus.ACTIVE,EmployeeStatus.ACTIVE,null,null,false,ApprovalRequestStatus.APPROVED,
                Set.of(scope,"DEVICE_READ_ALL"));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account);
        when(currentAccountPortIn.getCurrentAccount()).thenReturn(Optional.of(account));
        when(currentAccountPortIn.hasPermission(anyString())).thenAnswer(call -> account.getEffectivePermissionCodes().contains(call.getArgument(0)));
    }

    @ParameterizedTest
    @ValueSource(strings={"EMPLOYEE","PARKING_MANAGER","PARTNER_ADMIN"})
    void deviceReaderLoadsLaneFilterWithoutTopologyOrCheckInPermissions(String role) {
        deviceReader(role);
        jdbc.update("update parking.lanes set status='MAINTENANCE' where code='OUT' and gate_id in (select gate_id from parking.gates where zone_id=?)",zoneId);
        var options = deviceController.laneOptions(null).getBody().getData();
        assertEquals(2, options.size());
        assertTrue(options.stream().allMatch(lane -> lane.parkingLotId().equals(lotId)));
        assertTrue(options.stream().allMatch(lane -> lane.gateName().equals("New gate") && lane.zoneName().equals("New zone")));
        assertTrue(options.stream().anyMatch(lane -> lane.status().name().equals("MAINTENANCE")));
        assertEquals(2,deviceController.laneOptions(lotId).getBody().getData().size());
        assertThrows(AccessDeniedException.class,()->controller.zones(lotId,null));
        assertThrows(AccessDeniedException.class,()->adminController.activateLevel(levelId));
    }

    @ParameterizedTest
    @ValueSource(strings={"EMPLOYEE","PARKING_MANAGER","PARTNER_ADMIN"})
    void deviceLaneOptionsRejectInaccessibleNewLot(String role) {
        deviceReader(role);
        UUID otherOrg=UUID.randomUUID(), otherLot=UUID.randomUUID();
        jdbc.update("insert into iam.organizations(organization_id,code,name,status) values (?,?,?,'ACTIVE')",otherOrg,"ORG-"+otherOrg,"Other partner");
        insertLot(otherLot,otherOrg);
        assertThrows(AccessDeniedException.class,()->deviceController.laneOptions(otherLot));
        if (!role.equals("PARTNER_ADMIN")) {
            UUID unassignedLot=UUID.randomUUID(); insertLot(unassignedLot,organizationId);
            assertThrows(AccessDeniedException.class,()->deviceController.laneOptions(unassignedLot));
        }
    }

    @Test void deviceLaneOptionsForNewEmptyLotAreEmpty() {
        deviceReader("PARTNER_ADMIN");
        UUID newLot=UUID.randomUUID(); insertLot(newLot,organizationId);
        assertTrue(deviceController.laneOptions(newLot).getBody().getData().isEmpty());
    }

    @Test void checkInPermissionDoesNotGrantDeviceLaneLookup() {
        actor("EMPLOYEE",true);
        assertThrows(AccessDeniedException.class,()->deviceController.laneOptions(lotId));
    }
}
