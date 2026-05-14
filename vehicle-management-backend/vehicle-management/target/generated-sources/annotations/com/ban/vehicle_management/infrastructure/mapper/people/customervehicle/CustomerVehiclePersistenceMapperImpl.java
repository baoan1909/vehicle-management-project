package com.ban.vehicle_management.infrastructure.mapper.people.customervehicle;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.infrastructure.persistence.people.customervehicle.CustomerVehicleEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class CustomerVehiclePersistenceMapperImpl implements CustomerVehiclePersistenceMapper {

    @Override
    public CustomerVehicleEntity toEntity(CustomerVehicle domain) {
        if ( domain == null ) {
            return null;
        }

        CustomerVehicleEntity customerVehicleEntity = new CustomerVehicleEntity();

        customerVehicleEntity.setCreatedAt( domain.getCreatedAt() );
        customerVehicleEntity.setCreatedBy( domain.getCreatedBy() );
        customerVehicleEntity.setUpdatedAt( domain.getUpdatedAt() );
        customerVehicleEntity.setUpdatedBy( domain.getUpdatedBy() );
        customerVehicleEntity.setCustomerVehicleId( domain.getCustomerVehicleId() );
        customerVehicleEntity.setCustomerId( domain.getCustomerId() );
        customerVehicleEntity.setVehicleTypeId( domain.getVehicleTypeId() );
        customerVehicleEntity.setLicensePlate( domain.getLicensePlate() );
        customerVehicleEntity.setBrand( domain.getBrand() );
        customerVehicleEntity.setColor( domain.getColor() );
        customerVehicleEntity.setIsDefault( domain.getIsDefault() );
        customerVehicleEntity.setStatus( domain.getStatus() );

        return customerVehicleEntity;
    }

    @Override
    public CustomerVehicle toDomain(CustomerVehicleEntity entity) {
        if ( entity == null ) {
            return null;
        }

        CustomerVehicle customerVehicle = new CustomerVehicle();

        customerVehicle.setCreatedAt( entity.getCreatedAt() );
        customerVehicle.setCreatedBy( entity.getCreatedBy() );
        customerVehicle.setUpdatedAt( entity.getUpdatedAt() );
        customerVehicle.setUpdatedBy( entity.getUpdatedBy() );
        customerVehicle.setCustomerVehicleId( entity.getCustomerVehicleId() );
        customerVehicle.setCustomerId( entity.getCustomerId() );
        customerVehicle.setVehicleTypeId( entity.getVehicleTypeId() );
        customerVehicle.setLicensePlate( entity.getLicensePlate() );
        customerVehicle.setBrand( entity.getBrand() );
        customerVehicle.setColor( entity.getColor() );
        customerVehicle.setIsDefault( entity.getIsDefault() );
        customerVehicle.setStatus( entity.getStatus() );

        return customerVehicle;
    }
}
