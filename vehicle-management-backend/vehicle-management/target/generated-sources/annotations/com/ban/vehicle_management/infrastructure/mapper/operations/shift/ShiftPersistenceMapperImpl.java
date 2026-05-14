package com.ban.vehicle_management.infrastructure.mapper.operations.shift;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.infrastructure.persistence.operations.shift.ShiftEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class ShiftPersistenceMapperImpl implements ShiftPersistenceMapper {

    @Override
    public ShiftEntity toEntity(Shift domain) {
        if ( domain == null ) {
            return null;
        }

        ShiftEntity shiftEntity = new ShiftEntity();

        shiftEntity.setCreatedAt( domain.getCreatedAt() );
        shiftEntity.setCreatedBy( domain.getCreatedBy() );
        shiftEntity.setUpdatedAt( domain.getUpdatedAt() );
        shiftEntity.setUpdatedBy( domain.getUpdatedBy() );
        shiftEntity.setShiftId( domain.getShiftId() );
        shiftEntity.setShiftCode( domain.getShiftCode() );
        shiftEntity.setParkingLotId( domain.getParkingLotId() );
        shiftEntity.setStartTime( domain.getStartTime() );
        shiftEntity.setEndTime( domain.getEndTime() );
        shiftEntity.setStatus( domain.getStatus() );
        shiftEntity.setOpeningCash( domain.getOpeningCash() );
        shiftEntity.setClosingCash( domain.getClosingCash() );

        return shiftEntity;
    }

    @Override
    public Shift toDomain(ShiftEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Shift shift = new Shift();

        shift.setCreatedAt( entity.getCreatedAt() );
        shift.setCreatedBy( entity.getCreatedBy() );
        shift.setUpdatedAt( entity.getUpdatedAt() );
        shift.setUpdatedBy( entity.getUpdatedBy() );
        shift.setShiftId( entity.getShiftId() );
        shift.setShiftCode( entity.getShiftCode() );
        shift.setParkingLotId( entity.getParkingLotId() );
        shift.setStartTime( entity.getStartTime() );
        shift.setEndTime( entity.getEndTime() );
        shift.setStatus( entity.getStatus() );
        shift.setOpeningCash( entity.getOpeningCash() );
        shift.setClosingCash( entity.getClosingCash() );

        return shift;
    }
}
