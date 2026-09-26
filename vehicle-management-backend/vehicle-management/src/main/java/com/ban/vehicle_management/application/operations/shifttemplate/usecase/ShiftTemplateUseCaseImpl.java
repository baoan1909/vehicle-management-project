package com.ban.vehicle_management.application.operations.shifttemplate.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.application.operations.shifttemplate.port.in.ShiftTemplatePortIn;
import com.ban.vehicle_management.application.operations.shifttemplate.port.out.ShiftTemplatePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.domain.operations.shifttemplate.policy.ShiftTemplatePolicy;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftTemplateUseCaseImpl implements ShiftTemplatePortIn {

    private static final String SHIFT_CREATE_ALL = "SHIFT_CREATE_ALL";
    private static final String SHIFT_READ_ALL = "SHIFT_READ_ALL";
    private static final String SHIFT_UPDATE_ALL = "SHIFT_UPDATE_ALL";
    private static final String SHIFT_DELETE_ALL = "SHIFT_DELETE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ShiftTemplatePortOut shiftTemplatePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private ShiftTemplatePolicy shiftTemplatePolicy =
            new ShiftTemplatePolicy();

    public ShiftTemplateUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationAccessGuard organizationAccessGuard,
            ShiftTemplatePortOut shiftTemplatePortOut,
            ParkingLotPortOut parkingLotPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationAccessGuard = organizationAccessGuard;
        this.shiftTemplatePortOut = shiftTemplatePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
    }

    @Autowired
    void configureShiftTemplatePolicy(ShiftTemplatePolicy shiftTemplatePolicy) {
        this.shiftTemplatePolicy = shiftTemplatePolicy;
    }

    @Override
    @Transactional
    public ShiftTemplate createShiftTemplate(ShiftTemplate shiftTemplate) {
        currentAccountPortIn.requirePermission(SHIFT_CREATE_ALL);
        shiftTemplatePolicy.initialize(shiftTemplate);

        ensureParkingLotAvailable(shiftTemplate.getParkingLotId());
        ensureNoDuplicateActiveShiftType(shiftTemplate, null);
        ensureNoActiveTimeOverlap(shiftTemplate, null);

        shiftTemplate.setShiftTemplateId(UUID.randomUUID());
        return shiftTemplatePortOut.save(shiftTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftTemplate getShiftTemplateById(UUID shiftTemplateId) {
        currentAccountPortIn.requirePermission(SHIFT_READ_ALL);
        ShiftTemplate template = findExistingShiftTemplate(shiftTemplateId);
        requireLotScope(template.getParkingLotId(), false);
        return template;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftTemplate> getShiftTemplates(
            UUID parkingLotId,
            ShiftType shiftType,
            ShiftTemplateStatus status,
            String keyword
    ) {
        currentAccountPortIn.requirePermission(SHIFT_READ_ALL);
        String normalizedKeyword = normalizeKeyword(keyword);
        if (parkingLotId != null) {
            requireLotScope(parkingLotId, false);
            return shiftTemplatePortOut.findAll(parkingLotId, shiftType, status, normalizedKeyword);
        }
        String roleCode = currentAccountPortIn.getCurrentAccountOrThrow().roleCode();
        if (OrganizationAccessGuard.SYSTEM_ADMIN.equals(roleCode)) {
            return shiftTemplatePortOut.findAll(null, shiftType, status, normalizedKeyword);
        }
        if (!OrganizationAccessGuard.PARTNER_ADMIN.equals(roleCode)
                && !OrganizationAccessGuard.PARKING_MANAGER.equals(roleCode)) {
            throw new AccessDeniedException("Current account has no parking lot monitoring scope");
        }
        ParkingLotAccessScope scope = organizationAccessGuard.resolveParkingLotAccessScope();
        Set<UUID> lotIds = OrganizationAccessGuard.PARTNER_ADMIN.equals(roleCode)
                ? parkingLotPortOut.findAll(null, null, scope.organizationIds(), null).stream()
                        .map(ParkingLot::getParkingLotId).collect(Collectors.toSet())
                : scope.parkingLotIds();
        return lotIds.stream().flatMap(lotId -> shiftTemplatePortOut.findAll(
                lotId, shiftType, status, normalizedKeyword).stream()).toList();
    }

    @Override
    @Transactional
    public ShiftTemplate updateShiftTemplate(
            UUID shiftTemplateId,
            ShiftTemplate request
    ) {
        currentAccountPortIn.requirePermission(SHIFT_UPDATE_ALL);

        ShiftTemplate existing =
                findExistingShiftTemplate(shiftTemplateId);
        requireLotScope(existing.getParkingLotId(), true);

        existing.setName(request.getName());
        existing.setStartLocalTime(request.getStartLocalTime());
        existing.setEndLocalTime(request.getEndLocalTime());

        shiftTemplatePolicy.validateState(existing);

        if (existing.getStatus() == ShiftTemplateStatus.ACTIVE) {
            ensureParkingLotAvailable(existing.getParkingLotId());
            ensureNoDuplicateActiveShiftType(existing, shiftTemplateId);
            ensureNoActiveTimeOverlap(existing, shiftTemplateId);
        }

        return shiftTemplatePortOut.save(existing);
    }

    @Override
    @Transactional
    public ShiftTemplate activateShiftTemplate(UUID shiftTemplateId) {
        currentAccountPortIn.requirePermission(SHIFT_UPDATE_ALL);

        ShiftTemplate existing =
                findExistingShiftTemplate(shiftTemplateId);
        requireLotScope(existing.getParkingLotId(), true);

        ensureParkingLotAvailable(existing.getParkingLotId());

        if (existing.getStatus() == ShiftTemplateStatus.ACTIVE) {
            return existing;
        }

        shiftTemplatePolicy.activate(existing);
        ensureNoDuplicateActiveShiftType(existing, shiftTemplateId);
        ensureNoActiveTimeOverlap(existing, shiftTemplateId);

        return shiftTemplatePortOut.save(existing);
    }

    @Override
    @Transactional
    public void deleteShiftTemplate(UUID shiftTemplateId) {
        currentAccountPortIn.requirePermission(SHIFT_DELETE_ALL);

        ShiftTemplate existing =
                findExistingShiftTemplate(shiftTemplateId);
        requireLotScope(existing.getParkingLotId(), true);

        if (existing.getStatus() == ShiftTemplateStatus.INACTIVE) {
            return;
        }

        shiftTemplatePolicy.deactivate(existing);
        shiftTemplatePortOut.save(existing);
    }

    private ShiftTemplate findExistingShiftTemplate(UUID shiftTemplateId) {
        return shiftTemplatePortOut.findById(shiftTemplateId)
                .orElseThrow(() ->
                        new NotFoundException("Shift template not found")
                );
    }

    private void ensureParkingLotAvailable(UUID parkingLotId) {
        ParkingLot parkingLot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() ->
                        new NotFoundException("Parking lot not found")
                );

        organizationAccessGuard.ensureCanOperateParkingLot(parkingLot);

        if (parkingLot.getStatus() == ParkingLotStatus.CLOSED) {
            throw new ConflictException(
                    "Cannot use shift template for a closed parking lot"
            );
        }
    }

    private void requireLotScope(UUID parkingLotId, boolean write) {
        ParkingLot parkingLot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        if (write) {
            organizationAccessGuard.ensureCanOperateParkingLot(parkingLot);
        } else {
            organizationAccessGuard.ensureCanAccessParkingLot(parkingLot);
        }
    }

    private void ensureNoDuplicateActiveShiftType(
            ShiftTemplate candidate,
            UUID excludedShiftTemplateId
    ) {
        boolean duplicate;

        if (excludedShiftTemplateId == null) {
            duplicate = shiftTemplatePortOut
                    .existsActiveByParkingLotIdAndShiftType(
                            candidate.getParkingLotId(),
                            candidate.getShiftType()
                    );
        } else {
            duplicate = shiftTemplatePortOut
                    .existsActiveByParkingLotIdAndShiftTypeAndIdNot(
                            candidate.getParkingLotId(),
                            candidate.getShiftType(),
                            excludedShiftTemplateId
                    );
        }

        if (duplicate) {
            throw new ConflictException(
                    "Active shift template type already exists in parking lot"
            );
        }
    }

    private void ensureNoActiveTimeOverlap(
            ShiftTemplate candidate,
            UUID excludedShiftTemplateId
    ) {
        List<ShiftTemplate> activeTemplates =
                shiftTemplatePortOut.findActiveByParkingLotId(
                        candidate.getParkingLotId()
                );

        boolean overlaps = activeTemplates.stream()
                .filter(existing ->
                        excludedShiftTemplateId == null
                                || !Objects.equals(
                                existing.getShiftTemplateId(),
                                excludedShiftTemplateId
                        )
                )
                .anyMatch(existing ->
                        shiftTemplatePolicy.overlaps(candidate, existing)
                );

        if (overlaps) {
            throw new ConflictException(
                    "Shift template time overlaps with another active template"
            );
        }
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
    }
}
