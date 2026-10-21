package com.ban.vehicle_management.application.iam.organizationprofile.model.result;

/** Read-only parking-lot totals that give a Partner Admin operational context. */
public record OrganizationProfileOperationalSummary(
        long totalParkingLots,
        long activeParkingLots,
        long setupParkingLots,
        long maintenanceParkingLots
) {
}
