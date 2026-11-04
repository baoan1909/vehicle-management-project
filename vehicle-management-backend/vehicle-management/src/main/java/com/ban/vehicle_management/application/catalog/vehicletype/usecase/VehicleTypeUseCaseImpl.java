package com.ban.vehicle_management.application.catalog.vehicletype.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.catalog.authorization.CatalogAccessGuard;
import com.ban.vehicle_management.application.catalog.vehicletype.model.VehicleTypeDeactivationImpact;
import com.ban.vehicle_management.application.catalog.vehicletype.port.in.VehicleTypePortIn;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.catalog.vehicletype.policy.VehicleTypePolicy;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleTypeUseCaseImpl implements VehicleTypePortIn {

    private static final String VEHICLE_TYPE_CREATE_ALL = "VEHICLE_TYPE_CREATE_ALL";
    private static final String VEHICLE_TYPE_READ_ALL = "VEHICLE_TYPE_READ_ALL";
    private static final String VEHICLE_TYPE_UPDATE_ALL = "VEHICLE_TYPE_UPDATE_ALL";
    private static final String VEHICLE_TYPE_DELETE_ALL = "VEHICLE_TYPE_DELETE_ALL";
    private static final String PARKING_SESSION_CHECK_IN_ALL = "PARKING_SESSION_CHECK_IN_ALL";
    private static final String PARKING_SESSION_CHECK_OUT_ALL = "PARKING_SESSION_CHECK_OUT_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final VehicleTypePortOut vehicleTypePort;
    private final CatalogAccessGuard catalogAccessGuard;
    private final VehicleTypePolicy vehicleTypePolicy = new VehicleTypePolicy();

    public VehicleTypeUseCaseImpl(CurrentAccountPortIn currentAccountPortIn, VehicleTypePortOut vehicleTypePort,
                                  CatalogAccessGuard catalogAccessGuard) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.vehicleTypePort = vehicleTypePort;
        this.catalogAccessGuard = catalogAccessGuard;
    }

    @Override
    @Transactional
    public VehicleType createVehicleType(VehicleType vehicleType) {
        return createVehicleType(vehicleType, null);
    }

    @Override
    @Transactional
    public VehicleType createVehicleType(VehicleType vehicleType, UUID parkingLotId) {
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_CREATE_ALL);
        vehicleTypePolicy.initialize(vehicleType);
        vehicleType.setOrganizationId(catalogAccessGuard.writableOrganizationId(parkingLotId));

        if (vehicleTypePort.existsByCodeInOrganization(vehicleType.getCode(), vehicleType.getOrganizationId())) {
            throw new ConflictException("Vehicle type code already exists");
        }

        vehicleType.setVehicleTypeId(UUID.randomUUID());
        return vehicleTypePort.save(vehicleType);
    }

    @Override
    @Transactional
    public VehicleType updateVehicleType(UUID vehicleTypeId, VehicleType vehicleType) {
        return updateVehicleType(vehicleTypeId, vehicleType, null);
    }

    @Override
    @Transactional
    public VehicleType updateVehicleType(UUID vehicleTypeId, VehicleType vehicleType, UUID parkingLotId) {
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_UPDATE_ALL);
        VehicleType existingVehicleType = findExistingVehicleType(vehicleTypeId);
        if (parkingLotId == null) {
            catalogAccessGuard.ensureWritable(existingVehicleType.getOrganizationId());
        } else {
            catalogAccessGuard.ensureWritable(existingVehicleType.getOrganizationId(), parkingLotId);
        }

        if (vehicleType.getIsActive() != null
                && !vehicleType.getIsActive().equals(existingVehicleType.getIsActive())) {
            throw new BadRequestException("Use the dedicated vehicle type activation or deactivation action");
        }
        existingVehicleType.setCode(vehicleType.getCode());
        existingVehicleType.setName(vehicleType.getName());
        existingVehicleType.setDescription(vehicleType.getDescription());

        vehicleTypePolicy.initialize(existingVehicleType);

        if (vehicleTypePort.existsByCodeInOrganizationExcludingId(existingVehicleType.getCode(),
                existingVehicleType.getOrganizationId(), vehicleTypeId)) {
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
        requireCatalogReadForOperation();
        return vehicleTypePort.findAll(isActive, catalogAccessGuard.visibleOrganizationIds());
    }

    @Override
    @Transactional
    public void deleteVehicleType(UUID vehicleTypeId) {
        deactivateVehicleType(vehicleTypeId, false);
    }

    @Override
    @Transactional
    public void deactivateVehicleType(UUID vehicleTypeId, boolean cascadePriceRules) {
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_DELETE_ALL);
        VehicleType existingVehicleType = findExistingVehicleType(vehicleTypeId);
        catalogAccessGuard.ensureWritable(existingVehicleType.getOrganizationId());
        if (Boolean.FALSE.equals(existingVehicleType.getIsActive())) {
            return;
        }

        VehicleTypeDeactivationImpact impact = buildDeactivationImpact(vehicleTypeId);
        rejectOperationalBlockers(impact);
        if (impact.requiresPriceRuleCascade() && !cascadePriceRules) {
            throw new ConflictException("Vehicle type is used by active price rules; confirm cascading deactivation");
        }
        if (impact.requiresPriceRuleCascade()) {
            vehicleTypePort.deactivateActivePriceRules(vehicleTypeId);
        }
        vehicleTypePort.detachVehicleTypeFromZones(vehicleTypeId);

        vehicleTypePolicy.deactivate(existingVehicleType);
        vehicleTypePort.save(existingVehicleType);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleTypeDeactivationImpact getDeactivationImpact(UUID vehicleTypeId) {
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_UPDATE_ALL);
        findExistingVehicleType(vehicleTypeId);
        return buildDeactivationImpact(vehicleTypeId);
    }

    @Override
    @Transactional
    public VehicleType activateVehicleType(UUID vehicleTypeId) {
        currentAccountPortIn.requirePermission(VEHICLE_TYPE_UPDATE_ALL);
        VehicleType existingVehicleType = findExistingVehicleType(vehicleTypeId);
        catalogAccessGuard.ensureWritable(existingVehicleType.getOrganizationId());
        if (Boolean.TRUE.equals(existingVehicleType.getIsActive())) {
            return existingVehicleType;
        }

        vehicleTypePolicy.activate(existingVehicleType);
        if (vehicleTypePort.existsByCodeInOrganizationExcludingId(existingVehicleType.getCode(),
                existingVehicleType.getOrganizationId(), vehicleTypeId)) {
            throw new ConflictException("Vehicle type code already exists");
        }

        return vehicleTypePort.save(existingVehicleType);
    }

    private VehicleType findExistingVehicleType(UUID vehicleTypeId) {
        VehicleType vehicleType = vehicleTypePort.findById(vehicleTypeId)
                .orElseThrow(() -> new NotFoundException("Vehicle type not found"));
        catalogAccessGuard.ensureReadable(vehicleType.getOrganizationId());
        return vehicleType;
    }

    private void requireCatalogReadForOperation() {
        if (currentAccountPortIn.hasPermission(VEHICLE_TYPE_READ_ALL)
                || currentAccountPortIn.hasPermission(PARKING_SESSION_CHECK_IN_ALL)
                || currentAccountPortIn.hasPermission(PARKING_SESSION_CHECK_OUT_ALL)) {
            return;
        }

        currentAccountPortIn.requirePermission(VEHICLE_TYPE_READ_ALL);
    }

    private VehicleTypeDeactivationImpact buildDeactivationImpact(UUID vehicleTypeId) {
        return new VehicleTypeDeactivationImpact(
                vehicleTypePort.countActivePriceRules(vehicleTypeId),
                vehicleTypePort.countActiveCustomerVehicles(vehicleTypeId),
                vehicleTypePort.countServiceObligationSubscriptions(vehicleTypeId, LocalDate.now()),
                vehicleTypePort.countOpenParkingSessions(vehicleTypeId),
                vehicleTypePort.countAssignedZones(vehicleTypeId)
        );
    }

    private void rejectOperationalBlockers(VehicleTypeDeactivationImpact impact) {
        if (impact.activeSubscriptionCount() > 0) {
            throw new ConflictException("Vehicle type still has active or paid subscriptions");
        }
        if (impact.openParkingSessionCount() > 0) {
            throw new ConflictException("Vehicle type still has open parking sessions");
        }
    }
}

