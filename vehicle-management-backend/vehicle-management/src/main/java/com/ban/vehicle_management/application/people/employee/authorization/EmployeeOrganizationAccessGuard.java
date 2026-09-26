package com.ban.vehicle_management.application.people.employee.authorization;

import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Prevents a Partner from rostering an Employee owned by another Partner. */
@Component
public class EmployeeOrganizationAccessGuard {

    private final EmployeePortOut employeePortOut;
    private final OrganizationPortOut organizationPortOut;
    private final ParkingLotPortOut parkingLotPortOut;

    public EmployeeOrganizationAccessGuard(
            EmployeePortOut employeePortOut,
            OrganizationPortOut organizationPortOut,
            ParkingLotPortOut parkingLotPortOut
    ) {
        this.employeePortOut = employeePortOut;
        this.organizationPortOut = organizationPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
    }

    public void ensureBelongsToParkingLot(UUID employeeId, UUID parkingLotId) {
        UUID employeeAccountId = employeePortOut.findAccountIdByEmployeeId(employeeId)
                .orElseThrow(() -> new AccessDeniedException("Employee has no account ownership"));
        ParkingLot lot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        if (!organizationPortOut.findActiveOrganizationIdsByAccountId(employeeAccountId)
                .contains(lot.getOrganizationId())) {
            throw new AccessDeniedException("Employee belongs to another Partner organization");
        }
    }
}
