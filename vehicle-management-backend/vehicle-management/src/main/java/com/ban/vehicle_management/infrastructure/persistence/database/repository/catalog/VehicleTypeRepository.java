package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.Set;
import java.util.Collection;

public interface VehicleTypeRepository extends JpaRepository<VehicleTypeEntity, UUID>, JpaSpecificationExecutor<VehicleTypeEntity> {

    @Modifying
    @Query(value = """
            insert into catalog.vehicle_types
                (vehicle_type_id, organization_id, canonical_vehicle_type_id,
                 code, name, description, is_active, lifecycle_status, source_catalog_id, created_at, created_by)
            select gen_random_uuid(), :organizationId, source.vehicle_type_id,
                   source.code, source.name, source.description, false, 'DRAFT', source.vehicle_type_id, now(),
                   (select created_by from iam.organizations where organization_id = :organizationId)
            from catalog.vehicle_types source
            join iam.organizations template on template.organization_id = source.organization_id
            where template.code = 'COPARKING_INTERNAL'
            """, nativeQuery = true)
    int copyInternalCatalog(@Param("organizationId") UUID organizationId);

    @Query(value = """
            select excluded.vehicle_type_id
            from catalog.parking_lot_vehicle_type_exclusions excluded
            where excluded.parking_lot_id = :parkingLotId
            union
            select item.vehicle_type_id from catalog.vehicle_types item
            join parking.parking_lots lot on lot.organization_id = item.organization_id
            where lot.parking_lot_id = :parkingLotId and lot.catalog_explicit_services = true
              and not exists (select 1 from catalog.parking_lot_vehicle_type_enrollments enabled
                  where enabled.parking_lot_id = lot.parking_lot_id and enabled.vehicle_type_id = item.vehicle_type_id)
            """, nativeQuery = true)
    Set<UUID> findExcludedIdsByParkingLotId(@Param("parkingLotId") UUID parkingLotId);

    @Query(value = "select vehicle_type_id from catalog.parking_lot_vehicle_type_exclusions where parking_lot_id = :parkingLotId", nativeQuery = true)
    Set<UUID> findExplicitlyDisabledIdsByParkingLotId(@Param("parkingLotId") UUID parkingLotId);

    @Query(value = """
            select not exists (
                select 1 from parking.parking_lots lot
                join catalog.vehicle_types item on item.organization_id = lot.organization_id
                where lot.parking_lot_id = :parkingLotId and (item.vehicle_type_id = :vehicleTypeId or item.canonical_vehicle_type_id = :vehicleTypeId)
                  and not exists (select 1 from catalog.parking_lot_vehicle_type_exclusions excluded
                      where excluded.parking_lot_id = lot.parking_lot_id and excluded.vehicle_type_id = item.vehicle_type_id)
                  and (lot.catalog_explicit_services = false or exists (
                      select 1 from catalog.parking_lot_vehicle_type_enrollments enabled
                      where enabled.parking_lot_id = lot.parking_lot_id and enabled.vehicle_type_id = item.vehicle_type_id))
            )
            """, nativeQuery = true)
    boolean existsParkingLotExclusion(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("vehicleTypeId") UUID vehicleTypeId);

    @Modifying
    @Query(value = """
            delete from catalog.parking_lot_vehicle_type_exclusions
            where parking_lot_id = :parkingLotId and vehicle_type_id = :vehicleTypeId
            """, nativeQuery = true)
    int deleteParkingLotExclusion(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("vehicleTypeId") UUID vehicleTypeId);

    @Modifying
    @Query(value = """
            insert into catalog.parking_lot_vehicle_type_exclusions
                (organization_id, parking_lot_id, vehicle_type_id)
            values (:organizationId, :parkingLotId, :vehicleTypeId)
            on conflict do nothing
            """, nativeQuery = true)
    int insertParkingLotExclusion(
            @Param("organizationId") UUID organizationId,
            @Param("parkingLotId") UUID parkingLotId,
            @Param("vehicleTypeId") UUID vehicleTypeId);

    @Query(value = "select item.* from catalog.vehicle_types item join iam.organizations org using (organization_id) where org.code = 'COPARKING_INTERNAL' and item.code = :code and item.is_active = true", nativeQuery = true)
    Optional<VehicleTypeEntity> findCanonicalByCode(@Param("code") String code);


    @Modifying
    @Query(value = "insert into catalog.parking_lot_vehicle_type_enrollments (organization_id, parking_lot_id, vehicle_type_id) values (:organizationId, :parkingLotId, :itemId) on conflict do nothing", nativeQuery = true)
    int enrollAtParkingLot(@Param("organizationId") UUID organizationId, @Param("parkingLotId") UUID parkingLotId, @Param("itemId") UUID itemId);

    @Modifying
    @Query(value = "delete from catalog.parking_lot_vehicle_type_enrollments where parking_lot_id = :parkingLotId and vehicle_type_id = :itemId", nativeQuery = true)
    int unenrollAtParkingLot(@Param("parkingLotId") UUID parkingLotId, @Param("itemId") UUID itemId);

    boolean existsByCode(String code);

    boolean existsByCodeAndOrganizationId(String code, UUID organizationId);

    boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId);

    boolean existsByCodeAndOrganizationIdAndVehicleTypeIdNot(String code, UUID organizationId, UUID vehicleTypeId);

    boolean existsByVehicleTypeIdAndIsActiveTrue(UUID vehicleTypeId);

    @Query("select vehicleType.code from VehicleTypeEntity vehicleType where vehicleType.vehicleTypeId = :vehicleTypeId")
    Optional<String> findCodeById(@Param("vehicleTypeId") UUID vehicleTypeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VehicleTypeEntity item set item.isActive = false where item.organizationId in :organizationIds and item.isActive = true")
    int deactivateAllByOrganizationIds(@Param("organizationIds") Collection<UUID> organizationIds);
}


