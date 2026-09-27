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

import com.ban.vehicle_management.application.catalog.authorization.CatalogAccessGuard;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
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
        requestVehicleType.setIsActive(false);

        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.of(existingVehicleType));
        when(vehicleTypePort.save(any(VehicleType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleType updatedVehicleType = vehicleTypeUseCase.updateVehicleType(vehicleTypeId, requestVehicleType);

        assertEquals("CAR", updatedVehicleType.getCode());
        assertEquals("Car", updatedVehicleType.getName());
        assertEquals("Updated", updatedVehicleType.getDescription());
        assertFalse(updatedVehicleType.getIsActive());
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

