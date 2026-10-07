package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationAudience;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.application.parking.parkinglot.port.in.ParkingLotPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.AdministrativeBoundaryPortOut;
import com.ban.vehicle_management.application.parking.location.port.in.ParkingLocationFeaturePortIn;
import com.ban.vehicle_management.application.parking.location.port.out.ParkingLocationPortOut;
import com.ban.vehicle_management.application.parking.location.model.ParkingLocationSearchResult;
import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkinglot.policy.ParkingLotPolicy;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.GeocodingStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
    private final ParkingLocationPortOut parkingLocationPortOut;
    private final ParkingLocationFeaturePortIn featurePortIn;
    private final AdministrativeBoundaryPortOut administrativeBoundaryPortOut;
    private final AuditLogPortOut auditLogPortOut;
    private final ParkingLotPolicy parkingLotPolicy = new ParkingLotPolicy();

    public ParkingLotUseCaseImpl(
            ParkingLotPortOut parkingLotPortOut,
            NotificationPortIn notificationPortIn,
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationAccessGuard organizationAccessGuard,
            ParkingLocationPortOut parkingLocationPortOut,
            ParkingLocationFeaturePortIn featurePortIn,
            AdministrativeBoundaryPortOut administrativeBoundaryPortOut,
            AuditLogPortOut auditLogPortOut
    ) {
        this.parkingLotPortOut = parkingLotPortOut;
        this.notificationPortIn = notificationPortIn;
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationAccessGuard = organizationAccessGuard;
        this.parkingLocationPortOut = parkingLocationPortOut;
        this.featurePortIn = featurePortIn;
        this.administrativeBoundaryPortOut = administrativeBoundaryPortOut;
        this.auditLogPortOut = auditLogPortOut;
    }

    @Override
    @Transactional
    public ParkingLot createParkingLot(ParkingLot parkingLot) {
        currentAccountPortIn.requirePermission(PARKING_LOT_CREATE_ALL);
        requireAdminAddressV2WhenUsed(parkingLot);
        parkingLot.setOrganizationId(
                organizationAccessGuard.resolveOrganizationIdForParkingLotCreation(parkingLot.getOrganizationId())
        );
        parkingLotPolicy.initialize(parkingLot);
        resetUntrustedGeocodingState(parkingLot);

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
        requireAdminAddressV2WhenUsed(parkingLot);
        ParkingLot existingParkingLot = getParkingLotById(parkingLotId);
        organizationAccessGuard.ensureCanManageParkingLot(existingParkingLot);

        Map<String, Object> before = locationSnapshot(existingParkingLot);
        existingParkingLot.setCode(parkingLot.getCode());
        existingParkingLot.setName(parkingLot.getName());
        existingParkingLot.setAddressInputScheme(parkingLot.getAddressInputScheme());
        existingParkingLot.setAddressDisplay(parkingLot.getAddressDisplay());
        existingParkingLot.setCurrentWardCode(parkingLot.getCurrentWardCode());
        existingParkingLot.setLegacyWardCode(parkingLot.getLegacyWardCode());
        existingParkingLot.setLatitude(parkingLot.getLatitude());
        existingParkingLot.setLongitude(parkingLot.getLongitude());
        existingParkingLot.setTotalCapacity(parkingLot.getTotalCapacity());

        parkingLotPolicy.initialize(existingParkingLot);
        boolean addressOrLocationChanged = !before.equals(locationSnapshot(existingParkingLot));
        if (addressOrLocationChanged) {
            resetUntrustedGeocodingState(existingParkingLot);
            existingParkingLot.setActivationRequestedAt(null);
            existingParkingLot.setActivationRequestedBy(null);
        }

        if (parkingLotPortOut.existsByOrganizationIdAndCodeAndParkingLotIdNot(
                existingParkingLot.getOrganizationId(),
                existingParkingLot.getCode(),
                parkingLotId
        )) {
            throw new ConflictException("Parking lot code already exists");
        }

        ParkingLot saved = parkingLotPortOut.save(existingParkingLot);
        if (addressOrLocationChanged) {
            writeLocationAudit(saved, "PARKING_LOT_ADDRESS_LOCATION_UPDATED", before, locationSnapshot(saved));
        }
        return saved;
    }

    @Override
    @Transactional
    public ParkingLot geocodeParkingLot(UUID parkingLotId) {
        currentAccountPortIn.requirePermission(PARKING_LOT_UPDATE_ALL);
        featurePortIn.requireAdminAddressV2();
        featurePortIn.requireGeocodingProvider();
        ParkingLot parkingLot = getParkingLotById(parkingLotId);
        organizationAccessGuard.ensureCanManageParkingLot(parkingLot);
        if (parkingLot.getAddressDisplay() == null || parkingLot.getAddressDisplay().isBlank()) {
            throw new ConflictException("Parking lot display address is required before geocoding");
        }

        Map<String, Object> before = locationSnapshot(parkingLot);
        List<ParkingLocationSearchResult> candidates;
        try {
            candidates = parkingLocationPortOut.search(parkingLot.getAddressDisplay());
        } catch (RuntimeException exception) {
            parkingLot.setGeocodingStatus(GeocodingStatus.FAILED);
            parkingLot.setGeocodedAt(Instant.now());
            ParkingLot failed = parkingLotPortOut.save(parkingLot);
            writeLocationAudit(failed, "PARKING_LOT_GEOCODING_FAILED", before, locationSnapshot(failed));
            return failed;
        }
        if (candidates.isEmpty()) {
            parkingLot.setGeocodingStatus(GeocodingStatus.FAILED);
            parkingLot.setGeocodedAt(Instant.now());
            ParkingLot failed = parkingLotPortOut.save(parkingLot);
            writeLocationAudit(failed, "PARKING_LOT_GEOCODING_FAILED", before, locationSnapshot(failed));
            return failed;
        }

        ParkingLocationSearchResult candidate = candidates.getFirst();
        parkingLot.setLatitude(candidate.latitude());
        parkingLot.setLongitude(candidate.longitude());
        parkingLot.setGeocodedAt(Instant.now());
        List<String> wardCodes = administrativeBoundaryPortOut.findCurrentWardCodes(
                candidate.latitude(),
                candidate.longitude()
        );
        boolean uniquelyResolved = wardCodes.size() == 1;
        if (uniquelyResolved) {
            String resolvedWardCode = wardCodes.getFirst();
            if (parkingLot.getAddressInputScheme()
                    == com.ban.vehicle_management.shared.enumeration.parking.AddressInputScheme.LEGACY) {
                parkingLot.setCurrentWardCode(resolvedWardCode);
            } else if (parkingLot.getCurrentWardCode() != null
                    && !parkingLot.getCurrentWardCode().equals(resolvedWardCode)) {
                uniquelyResolved = false;
            } else {
                parkingLot.setCurrentWardCode(resolvedWardCode);
            }
        }
        parkingLot.setGeocodingStatus(
                uniquelyResolved ? GeocodingStatus.RESOLVED : GeocodingStatus.NEEDS_REVIEW
        );
        parkingLot.setActivationRequestedAt(null);
        parkingLot.setActivationRequestedBy(null);
        ParkingLot saved = parkingLotPortOut.save(parkingLot);
        writeLocationAudit(saved, "PARKING_LOT_GEOCODED", before, locationSnapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public ParkingLot confirmParkingLotLocation(
            UUID parkingLotId,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        currentAccountPortIn.requirePermission(PARKING_LOT_UPDATE_ALL);
        featurePortIn.requireAdminAddressV2();
        ParkingLot parkingLot = getParkingLotById(parkingLotId);
        organizationAccessGuard.ensureCanManageParkingLot(parkingLot);
        Map<String, Object> before = locationSnapshot(parkingLot);

        parkingLot.setLatitude(latitude);
        parkingLot.setLongitude(longitude);
        parkingLotPolicy.validateState(parkingLot);
        List<String> wardCodes = administrativeBoundaryPortOut.findCurrentWardCodes(latitude, longitude);
        if (wardCodes.size() != 1) {
            throw new ConflictException("Location must fall inside exactly one current ward in Vietnam");
        }
        parkingLot.setCurrentWardCode(wardCodes.getFirst());
        parkingLot.setGeocodingStatus(GeocodingStatus.MANUAL_CONFIRMED);
        parkingLot.setGeocodedAt(Instant.now());
        parkingLot.setActivationRequestedAt(null);
        parkingLot.setActivationRequestedBy(null);
        ParkingLot saved = parkingLotPortOut.save(parkingLot);
        writeLocationAudit(saved, "PARKING_LOT_LOCATION_MANUALLY_CONFIRMED", before, locationSnapshot(saved));
        return saved;
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

        ensureLocationReadyForActivation(existingParkingLot);
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

        ensureLocationReadyForActivation(existingParkingLot);
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

    private void requireAdminAddressV2WhenUsed(ParkingLot parkingLot) {
        if (parkingLot != null && (
                parkingLot.getAddressInputScheme() != null
                || parkingLot.getCurrentWardCode() != null
                || parkingLot.getLegacyWardCode() != null
                || parkingLot.getLatitude() != null
                || parkingLot.getLongitude() != null
        )) {
            featurePortIn.requireAdminAddressV2();
        }
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

    private void resetUntrustedGeocodingState(ParkingLot parkingLot) {
        parkingLot.setGeocodedAt(null);
        parkingLot.setGeocodingStatus(
                parkingLot.getLatitude() == null || parkingLot.getLongitude() == null
                        ? GeocodingStatus.NOT_REQUESTED
                        : GeocodingStatus.NEEDS_REVIEW
        );
    }

    private void ensureLocationReadyForActivation(ParkingLot parkingLot) {
        if (parkingLot.getAddressDisplay() == null || parkingLot.getAddressDisplay().isBlank()
                || parkingLot.getLatitude() == null || parkingLot.getLongitude() == null
                || parkingLot.getCurrentWardCode() == null
                || (parkingLot.getGeocodingStatus() != GeocodingStatus.RESOLVED
                    && parkingLot.getGeocodingStatus() != GeocodingStatus.MANUAL_CONFIRMED)) {
            throw new ConflictException("Parking lot requires a verified address and location before activation");
        }
        List<String> wards = administrativeBoundaryPortOut.findCurrentWardCodes(
                parkingLot.getLatitude(),
                parkingLot.getLongitude()
        );
        if (wards.size() != 1 || !Objects.equals(wards.getFirst(), parkingLot.getCurrentWardCode())) {
            throw new ConflictException("Parking lot location does not match exactly one current ward in Vietnam");
        }
    }

    private Map<String, Object> locationSnapshot(ParkingLot parkingLot) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("addressInputScheme", nullableText(parkingLot.getAddressInputScheme()));
        values.put("addressDisplay", nullableText(parkingLot.getAddressDisplay()));
        values.put("currentWardCode", nullableText(parkingLot.getCurrentWardCode()));
        values.put("legacyWardCode", nullableText(parkingLot.getLegacyWardCode()));
        values.put("latitude", nullableText(parkingLot.getLatitude()));
        values.put("longitude", nullableText(parkingLot.getLongitude()));
        values.put("geocodingStatus", nullableText(parkingLot.getGeocodingStatus()));
        return values;
    }

    private String nullableText(Object value) {
        return value == null ? "" : value.toString();
    }

    private void writeLocationAudit(
            ParkingLot parkingLot,
            String action,
            Map<String, Object> oldData,
            Map<String, Object> newData
    ) {
        if (auditLogPortOut == null) {
            return;
        }
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(currentAccountPortIn.getCurrentAccountIdOrThrow());
        auditLog.setAction(action);
        auditLog.setTargetSchema("parking");
        auditLog.setTargetTable("parking_lots");
        auditLog.setTargetId(parkingLot.getParkingLotId());
        auditLog.setOldData(oldData);
        auditLog.setNewData(newData);
        auditLogPortOut.save(auditLog);
    }

}
