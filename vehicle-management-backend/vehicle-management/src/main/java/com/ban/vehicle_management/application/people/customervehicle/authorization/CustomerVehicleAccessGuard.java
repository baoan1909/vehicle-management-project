package com.ban.vehicle_management.application.people.customervehicle.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.Set;

@Component
public class CustomerVehicleAccessGuard {

    private static final String CREATE_ALL_PERMISSION = "CUSTOMER_VEHICLE_CREATE_ALL";
    private static final String CREATE_OWN_PERMISSION = "CUSTOMER_VEHICLE_CREATE_OWN";
    private static final String READ_ALL_PERMISSION = "CUSTOMER_VEHICLE_READ_ALL";
    private static final String READ_OWN_PERMISSION = "CUSTOMER_VEHICLE_READ_OWN";
    private static final String UPDATE_ALL_PERMISSION = "CUSTOMER_VEHICLE_UPDATE_ALL";
    private static final String UPDATE_OWN_PERMISSION = "CUSTOMER_VEHICLE_UPDATE_OWN";
    private static final String DELETE_ALL_PERMISSION = "CUSTOMER_VEHICLE_DELETE_ALL";
    private static final String DELETE_OWN_PERMISSION = "CUSTOMER_VEHICLE_DELETE_OWN";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingLotPortOut parkingLotPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;

    public CustomerVehicleAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut,
            OrganizationAccessGuard organizationAccessGuard,
            ParkingLotPortOut parkingLotPortOut,
            CustomerVehiclePortOut customerVehiclePortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.parkingLotPortOut = parkingLotPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
    }

    /** Null is platform-wide; own-account reads bypass the admin parking-lot filter. */
    public Set<UUID> visibleParkingLotIdsForRead() {
        if (!currentAccountPortIn.hasPermission(READ_ALL_PERMISSION)) {
            currentAccountPortIn.requirePermission(READ_OWN_PERMISSION);
            return null;
        }
        return organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
    }

    public UUID resolveCustomerIdForCreate(UUID requestedCustomerId) {
        if (currentAccountPortIn.hasPermission(CREATE_ALL_PERMISSION)) {
            ensurePlatformScope();
            return requestedCustomerId;
        }

        currentAccountPortIn.requirePermission(CREATE_OWN_PERMISSION);
        return resolveCurrentApprovedCustomerId();
    }

    public UUID resolveCustomerIdForRead(UUID requestedCustomerId) {
        if (currentAccountPortIn.hasPermission(READ_ALL_PERMISSION)) {
            return requestedCustomerId;
        }

        currentAccountPortIn.requirePermission(READ_OWN_PERMISSION);
        return resolveCurrentApprovedCustomerId();
    }

    public void ensureCanRead(CustomerVehicle customerVehicle) {
        if (currentAccountPortIn.hasPermission(READ_ALL_PERMISSION)) {
            Set<UUID> lotIds = visibleParkingLotIdsForRead();
            if (lotIds != null && !customerVehiclePortOut.existsInParkingLots(
                    customerVehicle.getCustomerVehicleId(), lotIds)) {
                throw new AccessDeniedException("Customer vehicle is outside the accessible parking lots");
            }
            return;
        }
        ensureCanAccessVehicle(customerVehicle, READ_ALL_PERMISSION, READ_OWN_PERMISSION);
    }

    public void ensureCanUpdate(CustomerVehicle customerVehicle) {
        ensureCanAccessVehicle(customerVehicle, UPDATE_ALL_PERMISSION, UPDATE_OWN_PERMISSION);
    }

    public void ensureCanDelete(CustomerVehicle customerVehicle) {
        ensureCanAccessVehicle(customerVehicle, DELETE_ALL_PERMISSION, DELETE_OWN_PERMISSION);
    }

    public void ensureCanActivateOrInactivate(CustomerVehicle customerVehicle) {
        ensureCanAccessVehicle(customerVehicle, UPDATE_ALL_PERMISSION, UPDATE_OWN_PERMISSION);

    }

    public void ensureCanBlock() {
        currentAccountPortIn.requirePermission(UPDATE_ALL_PERMISSION);
        ensurePlatformScope();
    }

    private void ensureCanAccessVehicle(
            CustomerVehicle customerVehicle,
            String allPermissionCode,
            String ownPermissionCode
    ) {
        if (currentAccountPortIn.hasPermission(allPermissionCode)) {
            ensurePlatformScope();
            return;
        }

        currentAccountPortIn.requirePermission(ownPermissionCode);
        UUID currentCustomerId = resolveCurrentApprovedCustomerId();
        if (!currentCustomerId.equals(customerVehicle.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private void ensurePlatformScope() {
        if (!currentAccountPortIn.getCurrentAccountOrThrow().getEffectivePermissionCodes()
                .contains(OrganizationAccessGuard.PARKING_SCOPE_PLATFORM)) {
            throw new AccessDeniedException("Customer vehicles are managed by the platform or their owners");
        }
    }

    private UUID resolveCurrentApprovedCustomerId() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();

        AccountProfileState profileState = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new AccessDeniedException("Access is denied"));

        if (profileState.customerId() == null
                || !CustomerStatus.ACTIVE.equals(profileState.customerStatus())
                || !CustomerApprovalStatus.APPROVED.equals(profileState.customerApprovalStatus())) {
            throw new AccessDeniedException("Access is denied");
        }

        return profileState.customerId();
    }
}
