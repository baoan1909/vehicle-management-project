package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationAudience;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.application.parking.parkinglot.port.in.ParkingLotPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkinglot.policy.ParkingLotPolicy;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingLotUseCaseImpl implements ParkingLotPortIn {

    private static final String PARKING_LOT_CREATE_ALL = "PARKING_LOT_CREATE_ALL";
    private static final String PARKING_LOT_READ_ALL = "PARKING_LOT_READ_ALL";
    private static final String PARKING_LOT_UPDATE_ALL = "PARKING_LOT_UPDATE_ALL";

    private final ParkingLotPortOut parkingLotPortOut;
    private final NotificationPortIn notificationPortIn;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingLotPolicy parkingLotPolicy = new ParkingLotPolicy();

    public ParkingLotUseCaseImpl(
            ParkingLotPortOut parkingLotPortOut,
            NotificationPortIn notificationPortIn,
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationAccessGuard organizationAccessGuard
    ) {
        this.parkingLotPortOut = parkingLotPortOut;
        this.notificationPortIn = notificationPortIn;
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationAccessGuard = organizationAccessGuard;
    }

    @Override
    @Transactional
    public ParkingLot createParkingLot(ParkingLot parkingLot) {
        currentAccountPortIn.requirePermission(PARKING_LOT_CREATE_ALL);
        parkingLot.setOrganizationId(
                organizationAccessGuard.resolveOrganizationIdForParkingLotCreation(parkingLot.getOrganizationId())
        );
        parkingLotPolicy.initialize(parkingLot);

        if (parkingLotPortOut.existsByOrganizationIdAndCode(parkingLot.getOrganizationId(), parkingLot.getCode())) {
            throw new ConflictException("Parking lot code already exists");
        }

        parkingLot.setParkingLotId(UUID.randomUUID());
        return parkingLotPortOut.save(parkingLot);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingLot getParkingLotById(UUID parkingLotId) {
        currentAccountPortIn.requirePermission(PARKING_LOT_READ_ALL);
        ParkingLot parkingLot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        organizationAccessGuard.ensureCanAccessParkingLot(parkingLot);
        return parkingLot;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLot> getParkingLots(ParkingLotStatus status, String keyword) {
        currentAccountPortIn.requirePermission(PARKING_LOT_READ_ALL);
        ParkingLotAccessScope scope = organizationAccessGuard.resolveParkingLotAccessScope();
        return parkingLotPortOut.findAll(
                status,
                normalizeKeyword(keyword),
                scope.unrestricted() || scope.organizationIds().isEmpty() ? null : scope.organizationIds(),
                scope.unrestricted() || !scope.organizationIds().isEmpty() ? null : scope.parkingLotIds()
        );
    }

    @Override
    @Transactional
    public ParkingLot updateParkingLot(UUID parkingLotId, ParkingLot parkingLot) {
        currentAccountPortIn.requirePermission(PARKING_LOT_UPDATE_ALL);
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);
        organizationAccessGuard.ensureCanManageParkingLot(existingParkingLot);

        existingParkingLot.setCode(parkingLot.getCode());
        existingParkingLot.setName(parkingLot.getName());
        existingParkingLot.setAddress(parkingLot.getAddress());
        existingParkingLot.setLatitude(parkingLot.getLatitude());
        existingParkingLot.setLongitude(parkingLot.getLongitude());
        existingParkingLot.setTotalCapacity(parkingLot.getTotalCapacity());

        parkingLotPolicy.initialize(existingParkingLot);

        if (parkingLotPortOut.existsByOrganizationIdAndCodeAndParkingLotIdNot(
                existingParkingLot.getOrganizationId(),
                existingParkingLot.getCode(),
                parkingLotId
        )) {
            throw new ConflictException("Parking lot code already exists");
        }

        return parkingLotPortOut.save(existingParkingLot);
    }

    @Override
    @Transactional
    public void deleteParkingLot(UUID parkingLotId) {
        currentAccountPortIn.requirePermission(PARKING_LOT_UPDATE_ALL);
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);
        organizationAccessGuard.ensureCanManageParkingLot(existingParkingLot);

        if (existingParkingLot.getStatus() == ParkingLotStatus.CLOSED) {
            return;
        }

        ensureNoActiveZones(parkingLotId);

        parkingLotPolicy.close(existingParkingLot);
        parkingLotPortOut.save(existingParkingLot);
    }

    @Override
    @Transactional
    public ParkingLot activateParkingLot(UUID parkingLotId) {
        currentAccountPortIn.requirePermission(PARKING_LOT_UPDATE_ALL);
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);
        organizationAccessGuard.ensureCanManageParkingLot(existingParkingLot);

        if (existingParkingLot.getActivationRequestedAt() == null) {
            throw new ConflictException("Parking manager must request activation before the Partner Admin can activate this parking lot");
        }

        if (!parkingLotPortOut.isReadyForActivation(parkingLotId)) {
            throw new ConflictException(
                    "Parking lot needs at least one active zone, gate, IN lane, and OUT lane before activation"
            );
        }

        parkingLotPolicy.activate(existingParkingLot);
        return parkingLotPortOut.save(existingParkingLot);
    }

    @Override
    @Transactional
    public ParkingLot requestParkingLotActivation(UUID parkingLotId) {
        currentAccountPortIn.requirePermission(PARKING_LOT_READ_ALL);
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);

        if (!organizationAccessGuard.isCurrentParkingManager()) {
            throw new AccessDeniedException("Only the assigned Parking Manager can request parking lot activation");
        }
        if (existingParkingLot.getStatus() != ParkingLotStatus.SETUP) {
            throw new ConflictException("Only a parking lot in setup status can request activation");
        }
        if (!parkingLotPortOut.isReadyForActivation(parkingLotId)) {
            throw new ConflictException(
                    "Parking lot needs at least one active zone, gate, IN lane, and OUT lane before requesting activation"
            );
        }

        existingParkingLot.setActivationRequestedAt(Instant.now());
        existingParkingLot.setActivationRequestedBy(currentAccountPortIn.getCurrentAccountOrThrow().accountId());
        return parkingLotPortOut.save(existingParkingLot);
    }

    @Override
    @Transactional
    public ParkingLot markParkingLotMaintenance(UUID parkingLotId) {
        currentAccountPortIn.requirePermission(PARKING_LOT_UPDATE_ALL);
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);
        organizationAccessGuard.ensureCanManageParkingLot(existingParkingLot);

        parkingLotPolicy.markMaintenance(existingParkingLot);
        ParkingLot savedParkingLot = parkingLotPortOut.save(existingParkingLot);
        notifyParkingLotMaintenance(savedParkingLot);
        return savedParkingLot;
    }

    @Override
    @Transactional
    public ParkingLot closeParkingLot(UUID parkingLotId) {
        currentAccountPortIn.requirePermission(PARKING_LOT_UPDATE_ALL);
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);
        organizationAccessGuard.ensureCanManageParkingLot(existingParkingLot);
        ensureNoActiveZones(parkingLotId);
        parkingLotPolicy.close(existingParkingLot);
        return parkingLotPortOut.save(existingParkingLot);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
    private void ensureNoActiveZones(UUID parkingLotId) {
        if (parkingLotPortOut.hasActiveZones(parkingLotId)) {
            throw new ConflictException("Parking lot has active zones");
        }
    }

    private void notifyParkingLotMaintenance(ParkingLot parkingLot) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                false,
                NotificationAudience.OPERATIONS,
                null,
                null,
                NotificationType.PARKING_LOT_MAINTENANCE,
                "Bãi xe bảo trì",
                "Bãi xe " + parkingLot.getName() + " đã chuyển sang trạng thái bảo trì.",
                null,
                "parking",
                "parking_lots",
                parkingLot.getParkingLotId()
        ));
    }

}
