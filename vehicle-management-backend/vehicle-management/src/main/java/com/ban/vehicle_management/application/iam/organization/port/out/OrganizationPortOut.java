package com.ban.vehicle_management.application.iam.organization.port.out;

import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OrganizationPortOut {
    Organization save(Organization organization);

    Optional<Organization> findById(UUID organizationId);

    List<Organization> findAllByIds(Set<UUID> organizationIds);

    List<Organization> findAll();

    boolean existsByCode(String code);

    boolean isActiveAccountWithRole(UUID accountId, String roleCode);

    Optional<UUID> findActiveMembershipId(UUID organizationId, UUID accountId);

    Set<UUID> findActiveOrganizationIdsByAccountId(UUID accountId);

    Set<UUID> findScopedParkingLotIdsByAccountId(UUID accountId);

    Set<UUID> findScopedParkingLotIdsByOrganizationIdAndAccountId(UUID organizationId, UUID accountId);

    void createActiveMembership(UUID organizationId, UUID accountId);

    void replaceParkingLotScopes(UUID organizationMembershipId, Set<UUID> parkingLotIds);

    boolean allParkingLotsBelongToOrganization(UUID organizationId, Set<UUID> parkingLotIds);

}
