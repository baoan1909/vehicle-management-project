package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {
    List<OrganizationEntity> findAllByStatusOrderByClosedAtDesc(
            OrganizationStatus status, Pageable pageable);

    Optional<OrganizationEntity> findByOrganizationIdAndStatus(
            UUID organizationId, OrganizationStatus status);
    boolean existsByCode(String code);

    Optional<OrganizationEntity> findByCode(String code);

    List<OrganizationEntity> findAllByOrganizationIdIn(Collection<UUID> organizationIds);

    @Query("""
        select organization from OrganizationEntity organization
        where organization.organizationId = :organizationId
          and organization.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.CLOSING,
              com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.CLOSED)
        """)
    Optional<OrganizationEntity> findBusinessVisibleById(@Param("organizationId") UUID organizationId);

    @Query("""
        select organization from OrganizationEntity organization
        where organization.code = :code
          and organization.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.CLOSING,
              com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.CLOSED)
        """)
    Optional<OrganizationEntity> findBusinessVisibleByCode(@Param("code") String code);

    @Query("""
        select organization from OrganizationEntity organization
        where organization.organizationId in :organizationIds
          and organization.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.CLOSING,
              com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.CLOSED)
        """)
    List<OrganizationEntity> findAllBusinessVisibleByIds(
            @Param("organizationIds") Collection<UUID> organizationIds);

    @Query("""
        select organization from OrganizationEntity organization
        where organization.status not in (
            com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.CLOSING,
            com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.CLOSED)
        """)
    List<OrganizationEntity> findAllBusinessVisible();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select organization from OrganizationEntity organization
        where organization.organizationId in :organizationIds
        order by organization.organizationId
        """)
    List<OrganizationEntity> findAllByIdsForUpdate(
            @Param("organizationIds") Collection<UUID> organizationIds);
}
