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
    public static final String PARKING_SESSION_PLATE_REVIEW_ALL = "PARKING_SESSION_PLATE_REVIEW_ALL";
    public static final String PARKING_SESSION_PLATE_OVERRIDE_ALL = "PARKING_SESSION_PLATE_OVERRIDE_ALL";

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
    }

    public void ensureCanCheckOut() {
        currentAccountPortIn.requirePermission(PARKING_SESSION_CHECK_OUT_ALL);
    }

    public void ensureCanOperateParkingLot(ParkingLot parkingLot) {
        organizationAccessGuard.ensureCanOperateParkingLot(parkingLot);
    }

    public void ensureCanUseCardInParkingLot(Card card, ParkingLot parkingLot) {
        if (!parkingLot.getParkingLotId().equals(card.getParkingLotId())) {
            throw new AccessDeniedException("Card is not assigned to this parking lot");
        }
    }

    public void ensureCanUseOcr() {
        var permissions = currentAccountPortIn.getCurrentAccountOrThrow().getEffectivePermissionCodes();
        if (permissions.contains(PARKING_SESSION_CHECK_IN_ALL)
                || permissions.contains(PARKING_SESSION_CHECK_OUT_ALL)) {
            return;
        }
        throw new AccessDeniedException("Access is denied");
    }

    public void ensureCanReviewUnknownPlate() {
        currentAccountPortIn.requirePermission(PARKING_SESSION_PLATE_REVIEW_ALL);
    }

    public void ensureCanOverridePlateIdentity() {
        currentAccountPortIn.requirePermission(PARKING_SESSION_PLATE_OVERRIDE_ALL);
    }
}
