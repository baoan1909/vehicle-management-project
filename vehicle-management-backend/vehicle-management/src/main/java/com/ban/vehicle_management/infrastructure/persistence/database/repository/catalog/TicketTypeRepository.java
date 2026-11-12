package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.TicketTypeEntity;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Set;
import java.util.Collection;

public interface TicketTypeRepository
        extends JpaRepository<TicketTypeEntity, UUID>, JpaSpecificationExecutor<TicketTypeEntity> {

    @Modifying
    @Query(value = """
            insert into catalog.ticket_types
                (ticket_type_id, organization_id, code, name, description, duration_days, status, created_at)
            select gen_random_uuid(), :organizationId, source.code, source.name, source.description,
                   source.duration_days, source.status, now()
            from catalog.ticket_types source
            join iam.organizations template on template.organization_id = source.organization_id
            where template.code = 'COPARKING_INTERNAL'
            """, nativeQuery = true)
    int copyInternalCatalog(@Param("organizationId") UUID organizationId);

    @Query(value = """
            select ticket_type_id
            from catalog.parking_lot_ticket_type_exclusions
            where parking_lot_id = :parkingLotId
            """, nativeQuery = true)
    Set<UUID> findExcludedIdsByParkingLotId(@Param("parkingLotId") UUID parkingLotId);

    @Query(value = """
            select exists (
                select 1 from catalog.parking_lot_ticket_type_exclusions
                where parking_lot_id = :parkingLotId and ticket_type_id = :ticketTypeId
            )
            """, nativeQuery = true)
    boolean existsParkingLotExclusion(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("ticketTypeId") UUID ticketTypeId);

    @Modifying
    @Query(value = """
            delete from catalog.parking_lot_ticket_type_exclusions
            where parking_lot_id = :parkingLotId and ticket_type_id = :ticketTypeId
            """, nativeQuery = true)
    int deleteParkingLotExclusion(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("ticketTypeId") UUID ticketTypeId);

    @Modifying
    @Query(value = """
            insert into catalog.parking_lot_ticket_type_exclusions
                (organization_id, parking_lot_id, ticket_type_id)
            values (:organizationId, :parkingLotId, :ticketTypeId)
            on conflict do nothing
            """, nativeQuery = true)
    int insertParkingLotExclusion(
            @Param("organizationId") UUID organizationId,
            @Param("parkingLotId") UUID parkingLotId,
            @Param("ticketTypeId") UUID ticketTypeId);

    boolean existsByCodeAndStatus(String code, TicketTypeStatus status);

    boolean existsByCodeAndStatusAndOrganizationId(String code, TicketTypeStatus status, UUID organizationId);

    boolean existsByCodeAndStatusAndTicketTypeIdNot(
            String code,
            TicketTypeStatus status,
            UUID ticketTypeId
    );

    boolean existsByCodeAndStatusAndOrganizationIdAndTicketTypeIdNot(
            String code, TicketTypeStatus status, UUID organizationId, UUID ticketTypeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update TicketTypeEntity item
        set item.status = com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus.INACTIVE
        where item.organizationId in :organizationIds
          and item.status <> com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus.INACTIVE
        """)
    int deactivateAllByOrganizationIds(@Param("organizationIds") Collection<UUID> organizationIds);
}
