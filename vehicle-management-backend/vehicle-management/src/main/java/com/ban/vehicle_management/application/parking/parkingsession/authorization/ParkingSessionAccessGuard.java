package com.ban.vehicle_management.application.parking.parkingsession.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import org.springframework.stereotype.Component;

@Component
public class ParkingSessionAccessGuard {

    public static final String PARKING_SESSION_CHECK_IN_ALL = "PARKING_SESSION_CHECK_IN_ALL";
    public static final String PARKING_SESSION_CHECK_OUT_ALL = "PARKING_SESSION_CHECK_OUT_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;

    public ParkingSessionAccessGuard(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public void ensureCanCheckIn() {
        currentAccountPortIn.requirePermission(PARKING_SESSION_CHECK_IN_ALL);
    }

    public void ensureCanCheckOut() {
        currentAccountPortIn.requirePermission(PARKING_SESSION_CHECK_OUT_ALL);
    }
}
