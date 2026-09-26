package com.ban.vehicle_management.application.iam.organization.port.in;

import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OrganizationPortIn {
    Organization createOrganization(Organization organization, UUID partnerAdminAccountId);

    List<Organization> getAccessibleOrganizations();

    Organization getOrganizationById(UUID organizationId);

    void assignParkingManager(UUID organizationId, UUID parkingManagerAccountId, Set<UUID> parkingLotIds);

    void assignParkingManagerToParkingLot(UUID organizationId, UUID parkingManagerAccountId, UUID parkingLotId);
}
