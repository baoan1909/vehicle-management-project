package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.GateEntity;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GateRepository extends JpaRepository<GateEntity, UUID>, JpaSpecificationExecutor<GateEntity> {

    boolean existsByZoneIdAndStatus(UUID zoneId, GateStatus status);

    boolean existsByZoneIdAndCode(UUID zoneId, String code);

    boolean existsByZoneIdAndCodeAndGateIdNot(UUID zoneId, String code, UUID gateId);

    @Query("""
            select count(gate) > 0
            from GateEntity gate
            join ZoneEntity zone on zone.zoneId = gate.zoneId
            where zone.parkingLotId = :parkingLotId
              and gate.status = com.ban.vehicle_management.shared.enumeration.parking.GateStatus.ACTIVE
            """)
    boolean existsActiveGateByParkingLotId(@Param("parkingLotId") UUID parkingLotId);

    @Query("""
    select count(gate) > 0
    from GateEntity gate
    join gate.zone zone
    join zone.parkingLot parkingLot
     where gate.gateId = :gateId
          and gate.status = com.ban.vehicle_management.shared.enumeration.parking.GateStatus.ACTIVE
          and zone.status = com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus.ACTIVE
          and parkingLot.status in (
              com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus.SETUP,
              com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus.ACTIVE
          )
        
     """)
    boolean existsOperationalGateById(@Param("gateId") UUID gateId);

    @Query("""
    select gate.zoneId
    from GateEntity gate
    where gate.gateId = :gateId
""")
    Optional<UUID> findZoneIdByGateId(@Param("gateId") UUID gateId);
}
