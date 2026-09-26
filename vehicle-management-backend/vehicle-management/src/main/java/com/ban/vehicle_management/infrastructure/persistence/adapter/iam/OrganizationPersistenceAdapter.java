package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.infrastructure.mapper.iam.OrganizationPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.MemberParkingLotScopeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationMembershipEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.MemberParkingLotScopeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationMembershipRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrganizationPersistenceAdapter implements OrganizationPortOut {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final MemberParkingLotScopeRepository memberParkingLotScopeRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final OrganizationPersistenceMapper organizationPersistenceMapper;

    public OrganizationPersistenceAdapter(
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository organizationMembershipRepository,
            MemberParkingLotScopeRepository memberParkingLotScopeRepository,
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            ParkingLotRepository parkingLotRepository,
            OrganizationPersistenceMapper organizationPersistenceMapper
    ) {
        this.organizationRepository = organizationRepository;
        this.organizationMembershipRepository = organizationMembershipRepository;
        this.memberParkingLotScopeRepository = memberParkingLotScopeRepository;
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.organizationPersistenceMapper = organizationPersistenceMapper;
    }

    @Override
    public Organization save(Organization organization) {
        return organizationPersistenceMapper.toDomain(
                organizationRepository.saveAndFlush(organizationPersistenceMapper.toEntity(organization))
        );
    }

    @Override
    public Optional<Organization> findById(UUID organizationId) {
        return organizationRepository.findById(organizationId).map(organizationPersistenceMapper::toDomain);
    }

    @Override
    public List<Organization> findAllByIds(Set<UUID> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        return organizationRepository.findAllByOrganizationIdIn(organizationIds).stream()
                .map(organizationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Organization> findAll() {
        return organizationRepository.findAll().stream().map(organizationPersistenceMapper::toDomain).toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return organizationRepository.existsByCode(code);
    }

    @Override
    public boolean isActiveAccountWithRole(UUID accountId, String roleCode) {
        return accountRepository.findById(accountId)
                .filter(account -> AccountStatus.ACTIVE.equals(account.getStatus()))
                .flatMap(account -> roleRepository.findById(account.getRoleId()))
                .map(role -> Boolean.TRUE.equals(role.getIsActive()) && roleCode.equals(role.getCode()))
                .orElse(false);
    }

    @Override
    public Optional<UUID> findActiveMembershipId(UUID organizationId, UUID accountId) {
        return organizationMembershipRepository.findByOrganizationIdAndAccountIdAndStatus(
                organizationId,
                accountId,
                OrganizationMembershipStatus.ACTIVE
        ).map(OrganizationMembershipEntity::getOrganizationMembershipId);
    }

    @Override
    public Set<UUID> findActiveOrganizationIdsByAccountId(UUID accountId) {
        return organizationMembershipRepository.findActiveOrganizationIdsByAccountId(
                accountId,
                OrganizationMembershipStatus.ACTIVE
        );
    }

    @Override
    public Set<UUID> findScopedParkingLotIdsByAccountId(UUID accountId) {
        return memberParkingLotScopeRepository.findActiveParkingLotIdsByAccountId(
                accountId,
                OrganizationMembershipStatus.ACTIVE
        );
    }

    @Override
    public Set<UUID> findScopedParkingLotIdsByOrganizationIdAndAccountId(UUID organizationId, UUID accountId) {
        return memberParkingLotScopeRepository.findActiveParkingLotIdsByOrganizationIdAndAccountId(
                organizationId,
                accountId,
                OrganizationMembershipStatus.ACTIVE
        );
    }

    @Override
    public void createActiveMembership(UUID organizationId, UUID accountId) {
        OrganizationMembershipEntity membership = new OrganizationMembershipEntity();
        membership.setOrganizationMembershipId(UUID.randomUUID());
        membership.setOrganizationId(organizationId);
        membership.setAccountId(accountId);
        membership.setStatus(OrganizationMembershipStatus.ACTIVE);
        organizationMembershipRepository.saveAndFlush(membership);
    }

    @Override
    public void replaceParkingLotScopes(UUID organizationMembershipId, Set<UUID> parkingLotIds) {
        memberParkingLotScopeRepository.deleteByOrganizationMembershipId(organizationMembershipId);
        List<MemberParkingLotScopeEntity> scopes = parkingLotIds.stream().map(parkingLotId -> {
            MemberParkingLotScopeEntity scope = new MemberParkingLotScopeEntity();
            scope.setMemberParkingLotScopeId(UUID.randomUUID());
            scope.setOrganizationMembershipId(organizationMembershipId);
            scope.setParkingLotId(parkingLotId);
            return scope;
        }).toList();
        memberParkingLotScopeRepository.saveAll(scopes);
    }

    @Override
    public boolean allParkingLotsBelongToOrganization(UUID organizationId, Set<UUID> parkingLotIds) {
        return parkingLotIds != null
                && !parkingLotIds.isEmpty()
                && parkingLotRepository.countByOrganizationIdAndParkingLotIdIn(
                        organizationId,
                        parkingLotIds
                ) == parkingLotIds.size();
    }

}
