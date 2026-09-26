package com.ban.vehicle_management.application.parking.parkingsession.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ParkingSessionAccessGuard {

    public static final String PARKING_SESSION_CHECK_IN_ALL = "PARKING_SESSION_CHECK_IN_ALL";
    public static final String PARKING_SESSION_CHECK_OUT_ALL = "PARKING_SESSION_CHECK_OUT_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final EmployeeParkingLotAccessGuard employeeParkingLotAccessGuard;

    public ParkingSessionAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationAccessGuard organizationAccessGuard,
            EmployeeParkingLotAccessGuard employeeParkingLotAccessGuard
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationAccessGuard = organizationAccessGuard;
        this.employeeParkingLotAccessGuard = employeeParkingLotAccessGuard;
    }

    public void ensureCanCheckIn() {
        currentAccountPortIn.requirePermission(PARKING_SESSION_CHECK_IN_ALL);
        ensureNotSystemAdmin();
    }

    public void ensureCanCheckOut() {
        currentAccountPortIn.requirePermission(PARKING_SESSION_CHECK_OUT_ALL);
        ensureNotSystemAdmin();
    }

    public void ensureCanOperateParkingLot(ParkingLot parkingLot) {
        String roleCode = currentAccountPortIn.getCurrentAccountOrThrow().roleCode();
        if (OrganizationAccessGuard.PARTNER_ADMIN.equals(roleCode)
                || OrganizationAccessGuard.PARKING_MANAGER.equals(roleCode)) {
            organizationAccessGuard.ensureCanAccessParkingLot(parkingLot);
            return;
        }
        if ("EMPLOYEE".equals(roleCode)) {
            employeeParkingLotAccessGuard.ensureCanOperate(parkingLot.getParkingLotId());
            return;
        }
        throw new AccessDeniedException("Current account cannot operate this parking lot");
    }

    public void ensureCanUseCardInParkingLot(Card card, ParkingLot parkingLot) {
        String roleCode = currentAccountPortIn.getCurrentAccountOrThrow().roleCode();
        if ((OrganizationAccessGuard.PARTNER_ADMIN.equals(roleCode)
                || OrganizationAccessGuard.PARKING_MANAGER.equals(roleCode)
                || "EMPLOYEE".equals(roleCode))
                && !parkingLot.getParkingLotId().equals(card.getParkingLotId())) {
            throw new AccessDeniedException("Card is not assigned to this parking lot");
        }
    }

    public void ensureCanUseOcr() {
        ensureNotSystemAdmin();
        if (currentAccountPortIn.hasPermission(PARKING_SESSION_CHECK_IN_ALL)
                || currentAccountPortIn.hasPermission(PARKING_SESSION_CHECK_OUT_ALL)) {
            return;
        }
        throw new AccessDeniedException("Access is denied");
    }

    private void ensureNotSystemAdmin() {
        if (OrganizationAccessGuard.SYSTEM_ADMIN.equals(currentAccountPortIn.getCurrentAccountOrThrow().roleCode())) {
            throw new AccessDeniedException("System Admin cannot operate partner parking lots");
        }
    }
}
