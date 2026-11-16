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
                (ticket_type_id, organization_id, code, name, description, duration_days, status, source_catalog_id, created_at, created_by)
            select gen_random_uuid(), :organizationId, source.code, source.name, source.description,
                   source.duration_days, 'DRAFT', source.ticket_type_id, now(),
                   (select created_by from iam.organizations where organization_id = :organizationId)
            from catalog.ticket_types source
            join iam.organizations template on template.organization_id = source.organization_id
            where template.code = 'COPARKING_INTERNAL'
            """, nativeQuery = true)
    int copyInternalCatalog(@Param("organizationId") UUID organizationId);

    @Query(value = """
            select excluded.ticket_type_id
            from catalog.parking_lot_ticket_type_exclusions excluded
            where excluded.parking_lot_id = :parkingLotId
            union
            select item.ticket_type_id from catalog.ticket_types item
            join parking.parking_lots lot on lot.organization_id = item.organization_id
            where lot.parking_lot_id = :parkingLotId and lot.catalog_explicit_services = true
              and not exists (select 1 from catalog.parking_lot_ticket_type_enrollments enabled
                  where enabled.parking_lot_id = lot.parking_lot_id and enabled.ticket_type_id = item.ticket_type_id)
            """, nativeQuery = true)
    Set<UUID> findExcludedIdsByParkingLotId(@Param("parkingLotId") UUID parkingLotId);

    @Query(value = """
            select not exists (
                select 1 from parking.parking_lots lot
                join catalog.ticket_types item on item.organization_id = lot.organization_id
                where lot.parking_lot_id = :parkingLotId and item.ticket_type_id = :ticketTypeId
                  and not exists (select 1 from catalog.parking_lot_ticket_type_exclusions excluded
                      where excluded.parking_lot_id = lot.parking_lot_id and excluded.ticket_type_id = item.ticket_type_id)
                  and (lot.catalog_explicit_services = false or exists (
                      select 1 from catalog.parking_lot_ticket_type_enrollments enabled
                      where enabled.parking_lot_id = lot.parking_lot_id and enabled.ticket_type_id = item.ticket_type_id))
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

    @Modifying
    @Query(value = "insert into catalog.parking_lot_ticket_type_enrollments (organization_id, parking_lot_id, ticket_type_id) values (:organizationId, :parkingLotId, :itemId) on conflict do nothing", nativeQuery = true)
    int enrollAtParkingLot(@Param("organizationId") UUID organizationId, @Param("parkingLotId") UUID parkingLotId, @Param("itemId") UUID itemId);

    @Modifying
    @Query(value = "delete from catalog.parking_lot_ticket_type_enrollments where parking_lot_id = :parkingLotId and ticket_type_id = :itemId", nativeQuery = true)
    int unenrollAtParkingLot(@Param("parkingLotId") UUID parkingLotId, @Param("itemId") UUID itemId);

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
