package com.ban.vehicle_management.application.parking.parkingsession.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.ParkingLotScopeResolver;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** An Employee can operate only lots where their own shift is currently open. */
@Component
public class EmployeeParkingLotAccessGuard {

    private final ParkingLotScopeResolver parkingLotScopeResolver;

    public EmployeeParkingLotAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            ParkingLotScopeResolver parkingLotScopeResolver
    ) {
        this.parkingLotScopeResolver = parkingLotScopeResolver;
    }

    public Set<UUID> activeParkingLotIds() {
        return parkingLotScopeResolver.resolveCurrentEmployeeOperableLots();
    }

    public void ensureCanOperate(UUID parkingLotId) {
        parkingLotScopeResolver.ensureEmployeeCanOperate(parkingLotId);
    }
}
