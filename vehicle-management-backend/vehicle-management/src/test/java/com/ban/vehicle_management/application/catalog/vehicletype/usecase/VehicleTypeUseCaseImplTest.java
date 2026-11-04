package com.ban.vehicle_management.application.catalog.vehicletype.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;

import com.ban.vehicle_management.application.catalog.authorization.CatalogAccessGuard;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class VehicleTypeUseCaseImplTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000009011");

    @Mock
    private CatalogAccessGuard catalogAccessGuard;

    @Mock
    private VehicleTypePortOut vehicleTypePort;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks
    private VehicleTypeUseCaseImpl vehicleTypeUseCase;

    @BeforeEach
    void setUp() {
        lenient().when(catalogAccessGuard.writableOrganizationId((UUID) null)).thenReturn(ORGANIZATION_ID);
        lenient().when(catalogAccessGuard.visibleOrganizationIds()).thenReturn(Set.of(ORGANIZATION_ID));
    }

    @Test
    void shouldCreateVehicleTypeWithDefaultActiveFlag() {
        VehicleType requestVehicleType = new VehicleType();
        requestVehicleType.setCode(" MOTORBIKE ");
        requestVehicleType.setName(" Motorbike ");
        requestVehicleType.setDescription(" Two-wheel vehicle ");

        when(vehicleTypePort.save(any(VehicleType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleType createdVehicleType = vehicleTypeUseCase.createVehicleType(requestVehicleType);

        verify(currentAccountPortIn).requirePermission("VEHICLE_TYPE_CREATE_ALL");
        assertEquals("MOTORBIKE", createdVehicleType.getCode());
        assertEquals("Motorbike", createdVehicleType.getName());
        assertEquals("Two-wheel vehicle", createdVehicleType.getDescription());
        assertEquals(ORGANIZATION_ID, createdVehicleType.getOrganizationId());
        assertTrue(createdVehicleType.getIsActive());
        verify(vehicleTypePort).save(any(VehicleType.class));
    }

    @Test
    void shouldRejectDuplicateVehicleTypeCodeOnCreate() {
        VehicleType requestVehicleType = new VehicleType();
        requestVehicleType.setCode("CAR");
        requestVehicleType.setName("Car");

        when(vehicleTypePort.existsByCodeInOrganization("CAR", ORGANIZATION_ID)).thenReturn(true);

        assertThrows(ConflictException.class, () -> vehicleTypeUseCase.createVehicleType(requestVehicleType));
        verify(vehicleTypePort, never()).save(any(VehicleType.class));
    }

    @Test
    void shouldUpdateVehicleType() {
        UUID vehicleTypeId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setCode("MOTORBIKE");
        existingVehicleType.setName("Motorbike");
        existingVehicleType.setDescription("Old");
        existingVehicleType.setIsActive(true);

        VehicleType requestVehicleType = new VehicleType();
        requestVehicleType.setCode("CAR");
        requestVehicleType.setName("Car");
        requestVehicleType.setDescription("Updated");
        requestVehicleType.setIsActive(true);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        when(vehicleTypePort.save(any(VehicleType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleType updatedVehicleType = vehicleTypeUseCase.updateVehicleType(vehicleTypeId, requestVehicleType);

        assertEquals("CAR", updatedVehicleType.getCode());
        assertEquals("Car", updatedVehicleType.getName());
        assertEquals("Updated", updatedVehicleType.getDescription());
        assertTrue(updatedVehicleType.getIsActive());
    }

    @Test
    void assignedManagerCannotUpdatePartnerWideVehicleTypeDefinition() {
        UUID vehicleTypeId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setOrganizationId(ORGANIZATION_ID);
        existingVehicleType.setCode("MOTORBIKE");
        existingVehicleType.setName("Motorbike");
        existingVehicleType.setIsActive(true);

        VehicleType requestVehicleType = new VehicleType();
        requestVehicleType.setCode("MOTORBIKE");
        requestVehicleType.setName("Renamed by manager");
        requestVehicleType.setIsActive(true);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        doThrow(new AccessDeniedException("partner scope required"))
                .when(catalogAccessGuard).ensureWritable(ORGANIZATION_ID);

        assertThrows(AccessDeniedException.class,
                () -> vehicleTypeUseCase.updateVehicleType(vehicleTypeId, requestVehicleType));

        verify(currentAccountPortIn).requirePermission("VEHICLE_TYPE_UPDATE_ALL");
        verify(vehicleTypePort, never()).save(any(VehicleType.class));
    }

    @Test
    void assignedManagerCanUpdateVehicleTypeThroughSelectedAssignedLot() {
        UUID vehicleTypeId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setOrganizationId(ORGANIZATION_ID);
        existingVehicleType.setCode("MOTORBIKE");
        existingVehicleType.setName("Motorbike");
        existingVehicleType.setIsActive(true);

        VehicleType requestVehicleType = new VehicleType();
        requestVehicleType.setCode("MOTORBIKE");
        requestVehicleType.setName("Xe máy");
        requestVehicleType.setIsActive(true);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        when(vehicleTypePort.save(any(VehicleType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleType updated = vehicleTypeUseCase.updateVehicleType(vehicleTypeId, requestVehicleType, parkingLotId);

        verify(currentAccountPortIn).requirePermission("VEHICLE_TYPE_UPDATE_ALL");
        verify(catalogAccessGuard).ensureWritable(ORGANIZATION_ID, parkingLotId);
        assertEquals("Xe máy", updated.getName());
    }

    @Test
    void shouldRejectStatusChangeThroughGenericUpdate() {
        UUID vehicleTypeId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setCode("MOTORBIKE");
        existingVehicleType.setName("Motorbike");
        existingVehicleType.setIsActive(true);

        VehicleType requestVehicleType = new VehicleType();
        requestVehicleType.setCode("MOTORBIKE");
        requestVehicleType.setName("Motorbike");
        requestVehicleType.setIsActive(false);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));

        assertThrows(BadRequestException.class,
                () -> vehicleTypeUseCase.updateVehicleType(vehicleTypeId, requestVehicleType));

        assertTrue(existingVehicleType.getIsActive());
        verify(vehicleTypePort, never()).save(any(VehicleType.class));
    }

    @Test
    void shouldReturnOrderedVehicleTypes() {
        when(vehicleTypePort.findAll(Boolean.TRUE, Set.of(ORGANIZATION_ID)))
                .thenReturn(List.of(new VehicleType(), new VehicleType()));

        List<VehicleType> vehicleTypes = vehicleTypeUseCase.getVehicleTypes(Boolean.TRUE);

        assertEquals(2, vehicleTypes.size());
        verify(currentAccountPortIn).requirePermission("VEHICLE_TYPE_READ_ALL");
        verify(vehicleTypePort).findAll(Boolean.TRUE, Set.of(ORGANIZATION_ID));
    }

    @Test
    void shouldSoftDeleteVehicleTypeBySettingInactive() {
        UUID vehicleTypeId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setCode("CAR");
        existingVehicleType.setName("Car");
        existingVehicleType.setIsActive(true);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        when(vehicleTypePort.save(any(VehicleType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vehicleTypeUseCase.deleteVehicleType(vehicleTypeId);

        ArgumentCaptor<VehicleType> vehicleTypeCaptor = ArgumentCaptor.forClass(VehicleType.class);
        verify(currentAccountPortIn).requirePermission("VEHICLE_TYPE_DELETE_ALL");
        verify(vehicleTypePort).save(vehicleTypeCaptor.capture());
        assertFalse(vehicleTypeCaptor.getValue().getIsActive());
    }

    @Test
    void shouldDeactivateActivePriceRulesWhenCascadeIsConfirmed() {
        UUID vehicleTypeId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setOrganizationId(ORGANIZATION_ID);
        existingVehicleType.setCode("MOTORBIKE");
        existingVehicleType.setName("Motorbike");
        existingVehicleType.setIsActive(true);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        when(vehicleTypePort.countActivePriceRules(vehicleTypeId)).thenReturn(6L);
        when(vehicleTypePort.save(any(VehicleType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vehicleTypeUseCase.deactivateVehicleType(vehicleTypeId, true);

        verify(vehicleTypePort).deactivateActivePriceRules(vehicleTypeId);
        verify(vehicleTypePort).detachVehicleTypeFromZones(vehicleTypeId);
        verify(vehicleTypePort).save(existingVehicleType);
        assertFalse(existingVehicleType.getIsActive());
    }

    @Test
    void shouldRequireConfirmationBeforeCascadingActivePriceRules() {
        UUID vehicleTypeId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setOrganizationId(ORGANIZATION_ID);
        existingVehicleType.setIsActive(true);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        when(vehicleTypePort.countActivePriceRules(vehicleTypeId)).thenReturn(2L);

        assertThrows(ConflictException.class,
                () -> vehicleTypeUseCase.deactivateVehicleType(vehicleTypeId, false));

        verify(vehicleTypePort, never()).deactivateActivePriceRules(vehicleTypeId);
        verify(vehicleTypePort, never()).detachVehicleTypeFromZones(vehicleTypeId);
        verify(vehicleTypePort, never()).save(any(VehicleType.class));
    }

    @Test
    void shouldBlockDeactivationWhileParkingSessionsRemainOpen() {
        UUID vehicleTypeId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setOrganizationId(ORGANIZATION_ID);
        existingVehicleType.setIsActive(true);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        when(vehicleTypePort.countOpenParkingSessions(vehicleTypeId)).thenReturn(1L);

        assertThrows(ConflictException.class,
                () -> vehicleTypeUseCase.deactivateVehicleType(vehicleTypeId, true));

        verify(vehicleTypePort, never()).deactivateActivePriceRules(vehicleTypeId);
        verify(vehicleTypePort, never()).detachVehicleTypeFromZones(vehicleTypeId);
        verify(vehicleTypePort, never()).save(any(VehicleType.class));
    }

    @Test
    void shouldDetachVehicleTypeFromActiveZonesInsteadOfBlockingDeactivation() {
        UUID vehicleTypeId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setOrganizationId(ORGANIZATION_ID);
        existingVehicleType.setIsActive(true);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        when(vehicleTypePort.countAssignedZones(vehicleTypeId)).thenReturn(2L);
        when(vehicleTypePort.save(any(VehicleType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vehicleTypeUseCase.deactivateVehicleType(vehicleTypeId, true);

        verify(vehicleTypePort).detachVehicleTypeFromZones(vehicleTypeId);
        verify(vehicleTypePort).save(existingVehicleType);
        assertFalse(existingVehicleType.getIsActive());
    }

    @Test
    void shouldReportAllDeactivationDependencies() {
        UUID vehicleTypeId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setOrganizationId(ORGANIZATION_ID);
        existingVehicleType.setIsActive(true);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        when(vehicleTypePort.countActivePriceRules(vehicleTypeId)).thenReturn(3L);
        when(vehicleTypePort.countActiveCustomerVehicles(vehicleTypeId)).thenReturn(4L);
        when(vehicleTypePort.countOpenParkingSessions(vehicleTypeId)).thenReturn(1L);
        when(vehicleTypePort.countAssignedZones(vehicleTypeId)).thenReturn(2L);

        var impact = vehicleTypeUseCase.getDeactivationImpact(vehicleTypeId);

        verify(currentAccountPortIn).requirePermission("VEHICLE_TYPE_UPDATE_ALL");
        verify(catalogAccessGuard).ensureReadable(ORGANIZATION_ID);
        assertEquals(3L, impact.activePriceRuleCount());
        assertEquals(4L, impact.activeCustomerVehicleCount());
        assertEquals(1L, impact.openParkingSessionCount());
        assertEquals(2L, impact.assignedZoneCount());
        assertTrue(impact.requiresPriceRuleCascade());
        assertTrue(impact.hasOperationalBlockers());
    }

    @Test
    void shouldActivateInactiveVehicleType() {
        UUID vehicleTypeId = UUID.randomUUID();
        VehicleType existingVehicleType = new VehicleType();
        existingVehicleType.setVehicleTypeId(vehicleTypeId);
        existingVehicleType.setCode(" car ");
        existingVehicleType.setName(" Car ");
        existingVehicleType.setIsActive(false);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        when(vehicleTypePort.save(any(VehicleType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleType activatedVehicleType = vehicleTypeUseCase.activateVehicleType(vehicleTypeId);

        verify(currentAccountPortIn).requirePermission("VEHICLE_TYPE_UPDATE_ALL");
        assertTrue(activatedVehicleType.getIsActive());
        assertEquals("CAR", activatedVehicleType.getCode());
    }

    @Test
    void shouldThrowWhenVehicleTypeDoesNotExist() {
        UUID vehicleTypeId = UUID.randomUUID();
        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> vehicleTypeUseCase.getVehicleTypeById(vehicleTypeId));
    }
}

