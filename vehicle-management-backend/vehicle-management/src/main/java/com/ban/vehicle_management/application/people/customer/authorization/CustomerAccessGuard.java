package com.ban.vehicle_management.application.people.customer.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class CustomerAccessGuard {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingLotPortOut parkingLotPortOut;
    private final CustomerPortOut customerPortOut;

    public CustomerAccessGuard(CurrentAccountPortIn currentAccountPortIn,
            OrganizationAccessGuard organizationAccessGuard, ParkingLotPortOut parkingLotPortOut,
            CustomerPortOut customerPortOut) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationAccessGuard = organizationAccessGuard;
        this.parkingLotPortOut = parkingLotPortOut;
        this.customerPortOut = customerPortOut;
    }

    public Set<UUID> visibleParkingLotIds() {
        currentAccountPortIn.requirePermission("CUSTOMER_READ_ALL");
        return organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
    }

    public Set<UUID> visibleParkingLotIds(UUID requestedParkingLotId) {
        Set<UUID> accessibleParkingLotIds = visibleParkingLotIds();
        if (requestedParkingLotId == null) {
            return accessibleParkingLotIds;
        }
        if (accessibleParkingLotIds != null && !accessibleParkingLotIds.contains(requestedParkingLotId)) {
            throw new AccessDeniedException("Parking lot is outside the accessible scope");
        }
        return Set.of(requestedParkingLotId);
    }

    public void ensureCanRead(UUID customerId) {
        Set<UUID> parkingLotIds = visibleParkingLotIds();
        if (parkingLotIds != null && !customerPortOut.existsInParkingLots(customerId, parkingLotIds)) {
            throw new AccessDeniedException("Customer is outside the accessible parking lots");
        }
    }

    public void ensureCanManage() {
        currentAccountPortIn.requirePermission("CUSTOMER_UPDATE_ALL");
        if (!currentAccountPortIn.getCurrentAccountOrThrow().getEffectivePermissionCodes()
                .contains(OrganizationAccessGuard.PARKING_SCOPE_PLATFORM)) {
            throw new AccessDeniedException("Customer profile is managed by the platform");
        }
    }
}
