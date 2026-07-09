package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.LaneEntity;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LaneRepository extends JpaRepository<LaneEntity, UUID>, JpaSpecificationExecutor<LaneEntity> {

    boolean existsByGateIdAndStatus(UUID gateId, LaneStatus status);

    boolean existsByGateIdAndCode(UUID gateId, String code);

    boolean existsByGateIdAndCodeAndLaneIdNot(UUID gateId, String code, UUID laneId);


    @Query("""
        select count(lane) > 0
        from LaneEntity lane
        join lane.gate gate
        where gate.zoneId = :zoneId
            and lane.laneId <> :excludedLaneId
            and lane.status = :status
            and lane.direction = :direction
""")
    boolean existsOtherLaneInZoneByStatusAndDirection (
            @Param("zoneId") UUID zoneId,
            @Param("excludedLaneId") UUID excludedLaneId,
            @Param("status") LaneStatus status,
            @Param("direction")LaneDirection direction
    );

    @Query("""
    select case when count(laneEntity) > 0 then true else false end
    from LaneEntity laneEntity
    join laneEntity.gate gateEntity
    join gateEntity.zone zoneEntity
    where laneEntity.laneId = :laneId
      and zoneEntity.parkingLotId = :parkingLotId
    """)
    boolean existsLaneInParkingLot(
            @Param("laneId") UUID laneId,
            @Param("parkingLotId") UUID parkingLotId
    );

    boolean existsByLaneIdAndStatusNot(UUID laneId, LaneStatus status);
}
