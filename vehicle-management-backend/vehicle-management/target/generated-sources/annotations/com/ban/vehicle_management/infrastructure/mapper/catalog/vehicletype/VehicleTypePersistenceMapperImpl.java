package com.ban.vehicle_management.infrastructure.mapper.catalog.vehicletype;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.infrastructure.persistence.catalog.vehicletype.VehicleTypeEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class VehicleTypePersistenceMapperImpl implements VehicleTypePersistenceMapper {

    @Override
    public VehicleTypeEntity toEntity(VehicleType domain) {
        if ( domain == null ) {
            return null;
        }

        VehicleTypeEntity vehicleTypeEntity = new VehicleTypeEntity();

        vehicleTypeEntity.setCreatedAt( domain.getCreatedAt() );
        vehicleTypeEntity.setCreatedBy( domain.getCreatedBy() );
        vehicleTypeEntity.setUpdatedAt( domain.getUpdatedAt() );
        vehicleTypeEntity.setUpdatedBy( domain.getUpdatedBy() );
        vehicleTypeEntity.setVehicleTypeId( domain.getVehicleTypeId() );
        vehicleTypeEntity.setCode( domain.getCode() );
        vehicleTypeEntity.setName( domain.getName() );
        vehicleTypeEntity.setDescription( domain.getDescription() );
        vehicleTypeEntity.setIsActive( domain.getIsActive() );

        return vehicleTypeEntity;
    }

    @Override
    public VehicleType toDomain(VehicleTypeEntity entity) {
        if ( entity == null ) {
            return null;
        }

        VehicleType vehicleType = new VehicleType();

        vehicleType.setCreatedAt( entity.getCreatedAt() );
        vehicleType.setCreatedBy( entity.getCreatedBy() );
        vehicleType.setUpdatedAt( entity.getUpdatedAt() );
        vehicleType.setUpdatedBy( entity.getUpdatedBy() );
        vehicleType.setVehicleTypeId( entity.getVehicleTypeId() );
        vehicleType.setCode( entity.getCode() );
        vehicleType.setName( entity.getName() );
        vehicleType.setDescription( entity.getDescription() );
        vehicleType.setIsActive( entity.getIsActive() );

        return vehicleType;
    }
}
