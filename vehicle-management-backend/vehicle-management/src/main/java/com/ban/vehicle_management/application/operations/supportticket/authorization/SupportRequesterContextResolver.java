package com.ban.vehicle_management.application.operations.supportticket.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportRequesterContext;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SupportRequesterContextResolver {

    private final CurrentAccountPortIn currentAccountPortIn;

    public SupportRequesterContextResolver(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public SupportRequesterContext resolve(String requiredPermission) {
        currentAccountPortIn.requirePermission(requiredPermission);
        CurrentAccountAccess access = currentAccountPortIn.getCurrentAccountOrThrow();
        return new SupportRequesterContext(
                access.accountId(), access.roleCode(), access.username(), access.email(),
                access.getEffectivePermissionCodes(), access.activeOrganizationIds(), access.assignedParkingLotIds()
        );
    }

    public void ensureScopeAllowed(SupportRequesterContext requester, UUID organizationId, UUID parkingLotId) {
        if (organizationId != null && !requester.organizationIds().contains(organizationId)) {
            throw new AccessDeniedException("Organization is outside the current account scope");
        }
        if (parkingLotId != null && !requester.parkingLotIds().contains(parkingLotId)) {
            throw new AccessDeniedException("Parking lot is outside the current account scope");
        }
    }
}
