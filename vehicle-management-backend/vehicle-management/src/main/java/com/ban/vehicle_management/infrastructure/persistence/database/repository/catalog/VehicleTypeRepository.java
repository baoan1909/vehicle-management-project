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

public interface VehicleTypeRepository extends JpaRepository<VehicleTypeEntity, UUID>, JpaSpecificationExecutor<VehicleTypeEntity> {

    @Modifying
    @Query(value = """
            insert into catalog.vehicle_types
                (vehicle_type_id, organization_id, canonical_vehicle_type_id,
                 code, name, description, is_active, created_at)
            select gen_random_uuid(), :organizationId, source.vehicle_type_id,
                   source.code, source.name, source.description, source.is_active, now()
            from catalog.vehicle_types source
            join iam.organizations template on template.organization_id = source.organization_id
            where template.code = 'COPARKING_INTERNAL'
            """, nativeQuery = true)
    int copyInternalCatalog(@Param("organizationId") UUID organizationId);

    @Query(value = """
            select vehicle_type_id
            from catalog.parking_lot_vehicle_type_exclusions
            where parking_lot_id = :parkingLotId
            """, nativeQuery = true)
    Set<UUID> findExcludedIdsByParkingLotId(@Param("parkingLotId") UUID parkingLotId);

    @Query(value = """
            select exists (
                select 1 from catalog.parking_lot_vehicle_type_exclusions
                where parking_lot_id = :parkingLotId and vehicle_type_id = :vehicleTypeId
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

    boolean existsByCode(String code);

    boolean existsByCodeAndOrganizationId(String code, UUID organizationId);

    boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId);

    boolean existsByCodeAndOrganizationIdAndVehicleTypeIdNot(String code, UUID organizationId, UUID vehicleTypeId);

    boolean existsByVehicleTypeIdAndIsActiveTrue(UUID vehicleTypeId);

    @Query("select vehicleType.code from VehicleTypeEntity vehicleType where vehicleType.vehicleTypeId = :vehicleTypeId")
    Optional<String> findCodeById(@Param("vehicleTypeId") UUID vehicleTypeId);
}


