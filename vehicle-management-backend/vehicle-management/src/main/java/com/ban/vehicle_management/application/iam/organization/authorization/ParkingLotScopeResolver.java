package com.ban.vehicle_management.application.iam.organization.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.ParkingLotAssignmentPortOut;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotManagementState;
import com.ban.vehicle_management.shared.exception.ScopedAccessDeniedException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Single source of truth for effective Manager and Employee parking-lot scopes. */
@Component
public class ParkingLotScopeResolver {
    private final CurrentAccountPortIn currentAccountPortIn;
    private final ParkingLotAssignmentPortOut assignmentPortOut;

    public ParkingLotScopeResolver(
            CurrentAccountPortIn currentAccountPortIn,
            ParkingLotAssignmentPortOut assignmentPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.assignmentPortOut = assignmentPortOut;
    }

    public Set<UUID> resolveManagerManagedLots(UUID accountId) {
        return assignmentPortOut.findManagerManagedLotIds(accountId, Instant.now());
    }

    public Set<UUID> resolveEmployeeReadableLots(UUID accountId) {
        return assignmentPortOut.findEmployeeReadableLotIds(accountId, Instant.now());
    }

    public Set<UUID> resolveEmployeeOperableLots(UUID accountId) {
        return assignmentPortOut.findEmployeeOperableLotIds(accountId, Instant.now());
    }

    public Set<UUID> resolveCurrentEmployeeReadableLots() {
        return resolveEmployeeReadableLots(currentAccountPortIn.getCurrentAccountIdOrThrow());
    }

    public Set<UUID> resolveCurrentEmployeeOperableLots() {
        return resolveEmployeeOperableLots(currentAccountPortIn.getCurrentAccountIdOrThrow());
    }

    public boolean canManageEmployeeAtLot(UUID managerId, UUID employeeId, UUID parkingLotId) {
        return resolveManagerManagedLots(managerId).contains(parkingLotId)
                && assignmentPortOut.findAssignedEmployeeIds(parkingLotId, Instant.now()).contains(employeeId);
    }

    public boolean canManageEmployee(UUID managerId, UUID employeeId) {
        return resolveManagerManagedLots(managerId).stream()
                .anyMatch(lotId -> assignmentPortOut.findAssignedEmployeeIds(lotId, Instant.now())
                        .contains(employeeId));
    }

    public void ensureEmployeeCanRead(UUID parkingLotId) {
        if (parkingLotId == null || !resolveCurrentEmployeeReadableLots().contains(parkingLotId)) {
            throw new ScopedAccessDeniedException(
                    "PARKING_LOT_OUT_OF_SCOPE",
                    "Parking lot is outside the Employee readable scope");
        }
    }

    public void ensureEmployeeCanOperate(UUID parkingLotId) {
        if (parkingLotId == null || !resolveCurrentEmployeeReadableLots().contains(parkingLotId)) {
            throw new ScopedAccessDeniedException(
                    "PARKING_LOT_OUT_OF_SCOPE",
                    "Parking lot is outside the Employee readable scope");
        }
        if (ParkingLotManagementState.OPERATION_SUSPENDED.equals(
                assignmentPortOut.findParkingLotManagementState(parkingLotId))) {
            throw new ScopedAccessDeniedException(
                    "PARKING_LOT_OPERATION_SUSPENDED",
                    "Parking lot operations are suspended");
        }
        if (!resolveCurrentEmployeeOperableLots().contains(parkingLotId)) {
            throw new ScopedAccessDeniedException(
                    "ACTIVE_SHIFT_REQUIRED",
                    "An active open shift is required at this parking lot");
        }
    }
}
