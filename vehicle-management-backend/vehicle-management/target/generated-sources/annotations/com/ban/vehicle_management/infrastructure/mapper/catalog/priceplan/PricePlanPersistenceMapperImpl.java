package com.ban.vehicle_management.infrastructure.mapper.catalog.priceplan;

import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.infrastructure.persistence.catalog.priceplan.PricePlanEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class PricePlanPersistenceMapperImpl implements PricePlanPersistenceMapper {

    @Override
    public PricePlanEntity toEntity(PricePlan domain) {
        if ( domain == null ) {
            return null;
        }

        PricePlanEntity pricePlanEntity = new PricePlanEntity();

        pricePlanEntity.setCreatedAt( domain.getCreatedAt() );
        pricePlanEntity.setCreatedBy( domain.getCreatedBy() );
        pricePlanEntity.setUpdatedAt( domain.getUpdatedAt() );
        pricePlanEntity.setUpdatedBy( domain.getUpdatedBy() );
        pricePlanEntity.setPricePlanId( domain.getPricePlanId() );
        pricePlanEntity.setCode( domain.getCode() );
        pricePlanEntity.setName( domain.getName() );
        pricePlanEntity.setDescription( domain.getDescription() );
        pricePlanEntity.setAppliesTo( domain.getAppliesTo() );
        pricePlanEntity.setEffectiveFrom( domain.getEffectiveFrom() );
        pricePlanEntity.setEffectiveTo( domain.getEffectiveTo() );
        pricePlanEntity.setIsActive( domain.getIsActive() );

        return pricePlanEntity;
    }

    @Override
    public PricePlan toDomain(PricePlanEntity entity) {
        if ( entity == null ) {
            return null;
        }

        PricePlan pricePlan = new PricePlan();

        pricePlan.setCreatedAt( entity.getCreatedAt() );
        pricePlan.setCreatedBy( entity.getCreatedBy() );
        pricePlan.setUpdatedAt( entity.getUpdatedAt() );
        pricePlan.setUpdatedBy( entity.getUpdatedBy() );
        pricePlan.setPricePlanId( entity.getPricePlanId() );
        pricePlan.setCode( entity.getCode() );
        pricePlan.setName( entity.getName() );
        pricePlan.setDescription( entity.getDescription() );
        pricePlan.setAppliesTo( entity.getAppliesTo() );
        pricePlan.setEffectiveFrom( entity.getEffectiveFrom() );
        pricePlan.setEffectiveTo( entity.getEffectiveTo() );
        pricePlan.setIsActive( entity.getIsActive() );

        return pricePlan;
    }
}
