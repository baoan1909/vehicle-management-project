package com.ban.vehicle_management.application.operations.shifttemplate.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
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
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftTemplateUseCaseImpl implements ShiftTemplatePortIn {

    private static final String SHIFT_CREATE_ALL = "SHIFT_CREATE_ALL";
    private static final String SHIFT_READ_ALL = "SHIFT_READ_ALL";
    private static final String SHIFT_UPDATE_ALL = "SHIFT_UPDATE_ALL";
    private static final String SHIFT_DELETE_ALL = "SHIFT_DELETE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final ShiftTemplatePortOut shiftTemplatePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final ShiftTemplatePolicy shiftTemplatePolicy =
            new ShiftTemplatePolicy();

    public ShiftTemplateUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            ShiftTemplatePortOut shiftTemplatePortOut,
            ParkingLotPortOut parkingLotPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.shiftTemplatePortOut = shiftTemplatePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
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
        return findExistingShiftTemplate(shiftTemplateId);
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

        return shiftTemplatePortOut.findAll(
                parkingLotId,
                shiftType,
                status,
                normalizeKeyword(keyword)
        );
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

        if (parkingLot.getStatus() == ParkingLotStatus.CLOSED) {
            throw new ConflictException(
                    "Cannot use shift template for a closed parking lot"
            );
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