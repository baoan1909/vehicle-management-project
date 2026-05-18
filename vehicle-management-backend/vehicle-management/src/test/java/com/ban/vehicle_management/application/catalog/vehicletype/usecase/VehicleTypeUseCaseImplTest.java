package com.ban.vehicle_management.application.catalog.vehicletype.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleTypeUseCaseImplTest {

    @Mock
    private VehicleTypePortOut vehicleTypePort;

    @InjectMocks
    private VehicleTypeUseCaseImpl vehicleTypeUseCase;

    @Test
    void shouldCreateVehicleTypeWithDefaultActiveFlag() {
        VehicleType requestVehicleType = new VehicleType();
        requestVehicleType.setCode(" MOTORBIKE ");
        requestVehicleType.setName(" Motorbike ");
        requestVehicleType.setDescription(" Two-wheel vehicle ");

        when(vehicleTypePort.existsByCode("MOTORBIKE")).thenReturn(false);
        when(vehicleTypePort.save(any(VehicleType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleType createdVehicleType = vehicleTypeUseCase.createVehicleType(requestVehicleType);

        assertEquals("MOTORBIKE", createdVehicleType.getCode());
        assertEquals("Motorbike", createdVehicleType.getName());
        assertEquals("Two-wheel vehicle", createdVehicleType.getDescription());
        assertTrue(createdVehicleType.getIsActive());
        verify(vehicleTypePort).save(any(VehicleType.class));
    }

    @Test
    void shouldRejectDuplicateVehicleTypeCodeOnCreate() {
        VehicleType requestVehicleType = new VehicleType();
        requestVehicleType.setCode("CAR");
        requestVehicleType.setName("Car");

        when(vehicleTypePort.existsByCode("CAR")).thenReturn(true);

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
        when(vehicleTypePort.existsByCodeAndVehicleTypeIdNot("CAR", vehicleTypeId)).thenReturn(false);
        when(vehicleTypePort.save(any(VehicleType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleType updatedVehicleType = vehicleTypeUseCase.updateVehicleType(vehicleTypeId, requestVehicleType);

        assertEquals("CAR", updatedVehicleType.getCode());
        assertEquals("Car", updatedVehicleType.getName());
        assertEquals("Updated", updatedVehicleType.getDescription());
        assertFalse(updatedVehicleType.getIsActive());
    }

    @Test
    void shouldReturnOrderedVehicleTypes() {
        when(vehicleTypePort.findAll(Boolean.TRUE)).thenReturn(List.of(new VehicleType(), new VehicleType()));

        List<VehicleType> vehicleTypes = vehicleTypeUseCase.getVehicleTypes(Boolean.TRUE);

        assertEquals(2, vehicleTypes.size());
        verify(vehicleTypePort).findAll(Boolean.TRUE);
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
        verify(vehicleTypePort).save(vehicleTypeCaptor.capture());
        assertFalse(vehicleTypeCaptor.getValue().getIsActive());
    }

    @Test
    void shouldThrowWhenVehicleTypeDoesNotExist() {
        UUID vehicleTypeId = UUID.randomUUID();
        when(vehicleTypePort.findById(vehicleTypeId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> vehicleTypeUseCase.getVehicleTypeById(vehicleTypeId));
    }
}

