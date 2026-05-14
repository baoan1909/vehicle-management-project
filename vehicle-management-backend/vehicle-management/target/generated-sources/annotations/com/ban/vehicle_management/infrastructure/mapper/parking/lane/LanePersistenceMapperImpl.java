package com.ban.vehicle_management.infrastructure.mapper.parking.lane;

import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.infrastructure.persistence.parking.lane.LaneEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class LanePersistenceMapperImpl implements LanePersistenceMapper {

    @Override
    public LaneEntity toEntity(Lane domain) {
        if ( domain == null ) {
            return null;
        }

        LaneEntity laneEntity = new LaneEntity();

        laneEntity.setCreatedAt( domain.getCreatedAt() );
        laneEntity.setCreatedBy( domain.getCreatedBy() );
        laneEntity.setUpdatedAt( domain.getUpdatedAt() );
        laneEntity.setUpdatedBy( domain.getUpdatedBy() );
        laneEntity.setLaneId( domain.getLaneId() );
        laneEntity.setParkingLotId( domain.getParkingLotId() );
        laneEntity.setCode( domain.getCode() );
        laneEntity.setName( domain.getName() );
        laneEntity.setDirection( domain.getDirection() );
        laneEntity.setStatus( domain.getStatus() );

        return laneEntity;
    }

    @Override
    public Lane toDomain(LaneEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Lane lane = new Lane();

        lane.setCreatedAt( entity.getCreatedAt() );
        lane.setCreatedBy( entity.getCreatedBy() );
        lane.setUpdatedAt( entity.getUpdatedAt() );
        lane.setUpdatedBy( entity.getUpdatedBy() );
        lane.setLaneId( entity.getLaneId() );
        lane.setParkingLotId( entity.getParkingLotId() );
        lane.setCode( entity.getCode() );
        lane.setName( entity.getName() );
        lane.setDirection( entity.getDirection() );
        lane.setStatus( entity.getStatus() );

        return lane;
    }
}
