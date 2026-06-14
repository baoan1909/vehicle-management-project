package com.ban.vehicle_management.application.catalog.vehicletype.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
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

    private static final String VEHICLE_TYPE_CREATE_ALL = "VEHICLE_TYPE_CREATE_ALL";
    private static final String VEHICLE_TYPE_READ_ALL = "VEHICLE_TYPE_READ_ALL";
    private static final String VEHICLE_TYPE_UPDATE_ALL = "VEHICLE_TYPE_UPDATE_ALL";
    private static final String VEHICLE_TYPE_DELETE_ALL = "VEHICLE_TYPE_DELETE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final VehicleTypePortOut vehicleTypePort;
    private final VehicleTypePolicy vehicleTypePolicy = new VehicleTypePolicy();

    public VehicleTypeUseCaseImpl(CurrentAccountPortIn currentAccountPortIn, VehicleTypePortOut vehicleTypePort) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.vehicleTypePort = vehicleTypePort;
    }

    @Override
    @Transactional
    public VehicleType createVehicleType(VehicleType vehicleType) {
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_CREATE_ALL);
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
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_UPDATE_ALL);
        VehicleType existingVehicleType = findExistingVehicleType(vehicleTypeId);

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
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_READ_ALL);
        return findExistingVehicleType(vehicleTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleType> getVehicleTypes(Boolean isActive) {
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_READ_ALL);
        return vehicleTypePort.findAll(isActive);
    }

    @Override
    @Transactional
    public void deleteVehicleType(UUID vehicleTypeId) {
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_DELETE_ALL);
        VehicleType existingVehicleType = findExistingVehicleType(vehicleTypeId);
        if (Boolean.FALSE.equals(existingVehicleType.getIsActive())) {
            return;
        }
        rejectDeactivateWhenInUse(vehicleTypeId);

        vehicleTypePolicy.deactivate(existingVehicleType);
        vehicleTypePort.save(existingVehicleType);
    }

    @Override
    @Transactional
    public VehicleType activateVehicleType(UUID vehicleTypeId) {
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_UPDATE_ALL);
        VehicleType existingVehicleType = findExistingVehicleType(vehicleTypeId);
        if (Boolean.TRUE.equals(existingVehicleType.getIsActive())) {
            return existingVehicleType;
        }

        vehicleTypePolicy.activate(existingVehicleType);
        if (vehicleTypePort.existsByCodeAndVehicleTypeIdNot(existingVehicleType.getCode(), vehicleTypeId)) {
            throw new ConflictException("Vehicle type code already exists");
        }

        return vehicleTypePort.save(existingVehicleType);
    }

    private VehicleType findExistingVehicleType(UUID vehicleTypeId) {
        return vehicleTypePort.findById(vehicleTypeId)
                .orElseThrow(() -> new NotFoundException("Vehicle type not found"));
    }

    private void rejectDeactivateWhenInUse(UUID vehicleTypeId) {
        if (vehicleTypePort.hasActivePriceRules(vehicleTypeId)) {
            throw new ConflictException("Vehicle type is used by active price rules");
        }
        if (vehicleTypePort.hasActiveCustomerVehicles(vehicleTypeId)) {
            throw new ConflictException("Vehicle type is used by active customer vehicles");
        }
        if (vehicleTypePort.hasOpenParkingSessions(vehicleTypeId)) {
            throw new ConflictException("Vehicle type is used by open parking sessions");
        }
        if (vehicleTypePort.hasActiveCards(vehicleTypeId)) {
            throw new ConflictException("Vehicle type is used by active cards");
        }
        if (vehicleTypePort.hasActiveZones(vehicleTypeId)) {
            throw new ConflictException("Vehicle type is used by active zones");
        }
    }
}

