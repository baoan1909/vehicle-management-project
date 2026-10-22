package com.ban.vehicle_management.application.iam.organization.port.out;

import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OrganizationPortOut {
    Organization save(Organization organization);

    Optional<Organization> findById(UUID organizationId);

    Optional<Organization> findByCode(String code);

    List<Organization> findAllByIds(Set<UUID> organizationIds);

    List<Organization> findAll();

    boolean existsByCode(String code);

    boolean isActiveAccountWithRole(UUID accountId, String roleCode);

    Optional<UUID> findActiveMembershipId(UUID organizationId, UUID accountId);

    Set<UUID> findActiveOrganizationIdsByAccountId(UUID accountId);

    Set<UUID> findOrganizationIdsByAccountId(UUID accountId);

    Set<UUID> findScopedParkingLotIdsByAccountId(UUID accountId);

    Set<UUID> findScopedParkingLotIdsByOrganizationIdAndAccountId(UUID organizationId, UUID accountId);

    void createActiveMembership(UUID organizationId, UUID accountId);

    void replaceParkingLotScopes(UUID organizationMembershipId, Set<UUID> parkingLotIds);

    void addParkingLotScope(UUID organizationMembershipId, UUID parkingLotId);

    void removeParkingLotScope(UUID organizationMembershipId, UUID parkingLotId);

    Set<UUID> findActiveParkingManagerAccountIdsByParkingLotId(UUID parkingLotId);

    void deactivateMembershipsAndClearParkingLotScopes(UUID accountId);

    void activateMemberships(UUID accountId);

    boolean allParkingLotsBelongToOrganization(UUID organizationId, Set<UUID> parkingLotIds);

    Optional<UUID> findOrganizationIdByParkingLotId(UUID parkingLotId);

    long countParkingLotsByOrganizationId(UUID organizationId);

    long countParkingLotsByOrganizationIdAndStatus(UUID organizationId, ParkingLotStatus status);

}
