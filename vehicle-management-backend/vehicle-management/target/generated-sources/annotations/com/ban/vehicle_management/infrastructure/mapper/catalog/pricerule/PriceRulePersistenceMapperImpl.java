package com.ban.vehicle_management.infrastructure.mapper.catalog.pricerule;

import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.infrastructure.persistence.catalog.pricerule.PriceRuleEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class PriceRulePersistenceMapperImpl implements PriceRulePersistenceMapper {

    @Override
    public PriceRuleEntity toEntity(PriceRule domain) {
        if ( domain == null ) {
            return null;
        }

        PriceRuleEntity priceRuleEntity = new PriceRuleEntity();

        priceRuleEntity.setCreatedAt( domain.getCreatedAt() );
        priceRuleEntity.setCreatedBy( domain.getCreatedBy() );
        priceRuleEntity.setUpdatedAt( domain.getUpdatedAt() );
        priceRuleEntity.setUpdatedBy( domain.getUpdatedBy() );
        priceRuleEntity.setPriceRuleId( domain.getPriceRuleId() );
        priceRuleEntity.setPricePlanId( domain.getPricePlanId() );
        priceRuleEntity.setVehicleTypeId( domain.getVehicleTypeId() );
        priceRuleEntity.setTicketTypeId( domain.getTicketTypeId() );
        priceRuleEntity.setRuleName( domain.getRuleName() );
        priceRuleEntity.setTimeFrom( domain.getTimeFrom() );
        priceRuleEntity.setTimeTo( domain.getTimeTo() );
        priceRuleEntity.setBasePrice( domain.getBasePrice() );
        priceRuleEntity.setUnit( domain.getUnit() );
        priceRuleEntity.setLostCardFee( domain.getLostCardFee() );
        priceRuleEntity.setPriority( domain.getPriority() );

        return priceRuleEntity;
    }

    @Override
    public PriceRule toDomain(PriceRuleEntity entity) {
        if ( entity == null ) {
            return null;
        }

        PriceRule priceRule = new PriceRule();

        priceRule.setCreatedAt( entity.getCreatedAt() );
        priceRule.setCreatedBy( entity.getCreatedBy() );
        priceRule.setUpdatedAt( entity.getUpdatedAt() );
        priceRule.setUpdatedBy( entity.getUpdatedBy() );
        priceRule.setPriceRuleId( entity.getPriceRuleId() );
        priceRule.setPricePlanId( entity.getPricePlanId() );
        priceRule.setVehicleTypeId( entity.getVehicleTypeId() );
        priceRule.setTicketTypeId( entity.getTicketTypeId() );
        priceRule.setRuleName( entity.getRuleName() );
        priceRule.setTimeFrom( entity.getTimeFrom() );
        priceRule.setTimeTo( entity.getTimeTo() );
        priceRule.setBasePrice( entity.getBasePrice() );
        priceRule.setUnit( entity.getUnit() );
        priceRule.setLostCardFee( entity.getLostCardFee() );
        priceRule.setPriority( entity.getPriority() );

        return priceRule;
    }
}
