package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotManagementState;

public interface ParkingLotRepository extends JpaRepository<ParkingLotEntity, UUID>, JpaSpecificationExecutor<ParkingLotEntity> {

    @Query(value = """
        select exists (
            select 1 from parking.parking_lots lot where lot.parking_lot_id = :parkingLotId
            and (not lot.catalog_explicit_services or (
                exists (select 1 from catalog.parking_lot_vehicle_type_enrollments enrollment
                    join catalog.vehicle_types item using (vehicle_type_id)
                    where enrollment.parking_lot_id = lot.parking_lot_id and item.is_active)
                and exists (select 1 from catalog.parking_lot_ticket_type_enrollments enrollment
                    join catalog.ticket_types item using (ticket_type_id)
                    where enrollment.parking_lot_id = lot.parking_lot_id and item.status = 'ACTIVE')
                and not exists (
                    select 1 from catalog.parking_lot_vehicle_type_enrollments vehicles
                    join catalog.vehicle_types vehicle using (vehicle_type_id)
                    join catalog.parking_lot_ticket_type_enrollments tickets
                        on tickets.parking_lot_id = vehicles.parking_lot_id
                    join catalog.ticket_types ticket using (ticket_type_id)
                    where vehicles.parking_lot_id = lot.parking_lot_id
                      and vehicle.is_active and ticket.status = 'ACTIVE'
                      and not exists (
                          select 1 from catalog.price_rules rule
                          join catalog.price_plans plan using (price_plan_id)
                          where rule.organization_id = lot.organization_id
                            and rule.parking_lot_id = lot.parking_lot_id
                            and rule.vehicle_type_id = vehicle.vehicle_type_id
                            and rule.ticket_type_id = ticket.ticket_type_id
                            and rule.is_active and plan.is_active
                            and plan.effective_from <= (now() at time zone 'Asia/Ho_Chi_Minh')::date
                            and (plan.effective_to is null or plan.effective_to >= (now() at time zone 'Asia/Ho_Chi_Minh')::date)
                            and ((ticket.code <> 'DAILY' and plan.applies_to = 'CUSTOMER'
                                  and rule.time_from is null and rule.time_to is null)
                              or (ticket.code = 'DAILY' and plan.applies_to = 'VISITOR'
                                  and rule.time_from is not null and rule.time_to is not null))
                          group by rule.price_plan_id
                          having ticket.code <> 'DAILY' or sum(
                              case when rule.time_from < rule.time_to
                                  then extract(epoch from (rule.time_to - rule.time_from)) + 1
                                  else 86400 - extract(epoch from (rule.time_from - rule.time_to)) + 1 end
                          ) >= 86400
                      )
                )
            ))
        )
        """, nativeQuery = true)
    boolean hasPublishedCatalogForActivation(@Param("parkingLotId") UUID parkingLotId);

    @Query(value = "select extversion from pg_extension where extname='postgis'", nativeQuery = true)
    String findPostGisExtensionVersion();

    @Query(value = """
        select format_type(attribute.atttypid, attribute.atttypmod)
        from pg_attribute attribute
        join pg_class relation on relation.oid=attribute.attrelid
        join pg_namespace namespace on namespace.oid=relation.relnamespace
        where namespace.nspname='parking' and relation.relname='parking_lots'
          and attribute.attname='location' and not attribute.attisdropped
        limit 1
        """, nativeQuery = true)
    String findLocationColumnType();

    @Query(value = """
        select indexname from pg_indexes where schemaname='parking'
          and tablename='parking_lots' and indexname='idx_parking_lots_active_location_gist'
        limit 1
        """, nativeQuery = true)
    String findActiveLocationSpatialIndex();

    @Query(value = "select count(*) from parking.parking_lots where status='ACTIVE' and location is null",
            nativeQuery = true)
    long countActiveWithoutLocation();

    @Query(value = "select count(*) from parking.parking_lots where geocoding_status='NEEDS_REVIEW'",
            nativeQuery = true)
    long countAddressNeedsReview();

    @Query(value = """
            WITH search_point AS (
                SELECT public.ST_SetSRID(
                    public.ST_MakePoint(
                        CAST(:longitude AS double precision),
                        CAST(:latitude AS double precision)
                    ),
                    4326
                )::geography AS location
            )
            SELECT pl.parking_lot_id AS "parkingLotId",
                   pl.name AS "name",
                   pl.address_display AS "addressDisplay",
                   pl.latitude AS "latitude",
                   pl.longitude AS "longitude",
                   pl.status AS "status",
                   public.ST_Distance(pl.location, search_point.location) AS "distanceMeters"
            FROM parking.parking_lots pl
            CROSS JOIN search_point
            WHERE pl.status IN ('ACTIVE', 'SETUP', 'MAINTENANCE')
              AND pl.location IS NOT NULL
              AND public.ST_DWithin(
                  pl.location,
                  search_point.location,
                  CAST(:radiusMeters AS double precision)
              )
            ORDER BY "distanceMeters" ASC, pl.parking_lot_id ASC
            LIMIT :resultLimit
            """, nativeQuery = true)
    List<NearbyParkingLotProjection> findNearby(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusMeters") BigDecimal radiusMeters,
            @Param("resultLimit") int resultLimit);

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeAndParkingLotIdNot(
            UUID organizationId,
            String code,
            UUID parkingLotId
    );

    long countByOrganizationIdAndParkingLotIdIn(UUID organizationId, Collection<UUID> parkingLotIds);

    @Query("""
        select count(lot) from ParkingLotEntity lot, OrganizationEntity organization
        where lot.organizationId = :organizationId
          and lot.parkingLotId in :parkingLotIds
          and organization.organizationId = lot.organizationId
          and organization.status = com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.ACTIVE
          and lot.status <> com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus.CLOSED
        """)
    long countBusinessWritableByOrganizationIdAndParkingLotIdIn(
            @Param("organizationId") UUID organizationId,
            @Param("parkingLotIds") Collection<UUID> parkingLotIds);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, ParkingLotStatus status);

    @Query("""
        select lot from ParkingLotEntity lot, OrganizationEntity organization
        where lot.parkingLotId = :parkingLotId
          and organization.organizationId = lot.organizationId
          and lot.status <> com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus.CLOSED
          and organization.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.CLOSING,
              com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.CLOSED)
        """)
    Optional<ParkingLotEntity> findBusinessVisibleById(@Param("parkingLotId") UUID parkingLotId);

    boolean existsByParkingLotIdAndStatus(UUID parkingLotId, ParkingLotStatus status);

    boolean existsByParkingLotIdAndStatusIn(UUID parkingLotId, Set<ParkingLotStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lot from ParkingLotEntity lot where lot.parkingLotId = :parkingLotId")
    Optional<ParkingLotEntity> findByIdForUpdate(@Param("parkingLotId") UUID parkingLotId);

    @Modifying
    @Query("""
        update ParkingLotEntity lot
        set lot.managementState = :state,
            lot.managementRevision = lot.managementRevision + 1
        where lot.parkingLotId = :parkingLotId
        """)
    int updateManagementState(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("state") ParkingLotManagementState state);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ParkingLotEntity lot
        set lot.status = com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus.CLOSED
        where lot.organizationId in :organizationIds
          and lot.status <> com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus.CLOSED
        """)
    int closeAllByOrganizationIds(@Param("organizationIds") Collection<UUID> organizationIds);
}
