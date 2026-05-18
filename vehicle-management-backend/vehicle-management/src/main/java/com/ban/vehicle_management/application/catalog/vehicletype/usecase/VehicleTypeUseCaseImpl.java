package com.ban.vehicle_management.application.catalog.vehicletype.usecase;

import com.ban.vehicle_management.application.catalog.vehicletype.port.in.VehicleTypePortIn;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.catalog.vehicletype.policy.VehicleTypePolicy;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleTypeUseCaseImpl implements VehicleTypePortIn {

    private final VehicleTypePortOut vehicleTypePort;
    private final VehicleTypePolicy vehicleTypePolicy = new VehicleTypePolicy();

    public VehicleTypeUseCaseImpl(VehicleTypePortOut vehicleTypePort) {
        this.vehicleTypePort = vehicleTypePort;
    }

    @Override
    @Transactional
    public VehicleType createVehicleType(VehicleType vehicleType) {
        vehicleTypePolicy.initialize(vehicleType);

        if (vehicleTypePort.existsByCode(vehicleType.getCode())) {
            throw new ConflictException("Vehicle type code already exists");
        }

        vehicleType.setVehicleTypeId(UUID.randomUUID());
        return vehicleTypePort.save(vehicleType);
    }

    @Override
    @Transactional
    public VehicleType updateVehicleType(UUID vehicleTypeId, VehicleType vehicleType) {
        VehicleType existingVehicleType = getVehicleTypeById(vehicleTypeId);

        existingVehicleType.setCode(vehicleType.getCode());
        existingVehicleType.setName(vehicleType.getName());
        existingVehicleType.setDescription(vehicleType.getDescription());
        if (vehicleType.getIsActive() != null) {
            existingVehicleType.setIsActive(vehicleType.getIsActive());
        }

        vehicleTypePolicy.initialize(existingVehicleType);

        if (vehicleTypePort.existsByCodeAndVehicleTypeIdNot(existingVehicleType.getCode(), vehicleTypeId)) {
            throw new ConflictException("Vehicle type code already exists");
        }

        return vehicleTypePort.save(existingVehicleType);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleType getVehicleTypeById(UUID vehicleTypeId) {
        return vehicleTypePort.findById(vehicleTypeId)
                .orElseThrow(() -> new NotFoundException("Vehicle type not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleType> getVehicleTypes(Boolean isActive) {
        return vehicleTypePort.findAll(isActive);
    }

    @Override
    @Transactional
    public void deleteVehicleType(UUID vehicleTypeId) {
        VehicleType existingVehicleType = getVehicleTypeById(vehicleTypeId);
        if (Boolean.FALSE.equals(existingVehicleType.getIsActive())) {
            return;
        }

        vehicleTypePolicy.deactivate(existingVehicleType);
        vehicleTypePort.save(existingVehicleType);
    }
}

