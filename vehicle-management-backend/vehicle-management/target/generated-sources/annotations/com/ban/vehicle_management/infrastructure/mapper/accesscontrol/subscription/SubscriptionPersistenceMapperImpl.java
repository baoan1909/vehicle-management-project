package com.ban.vehicle_management.infrastructure.mapper.accesscontrol.subscription;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.subscription.SubscriptionEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class SubscriptionPersistenceMapperImpl implements SubscriptionPersistenceMapper {

    @Override
    public SubscriptionEntity toEntity(Subscription domain) {
        if ( domain == null ) {
            return null;
        }

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();

        subscriptionEntity.setCreatedAt( domain.getCreatedAt() );
        subscriptionEntity.setCreatedBy( domain.getCreatedBy() );
        subscriptionEntity.setUpdatedAt( domain.getUpdatedAt() );
        subscriptionEntity.setUpdatedBy( domain.getUpdatedBy() );
        subscriptionEntity.setSubscriptionId( domain.getSubscriptionId() );
        subscriptionEntity.setCustomerId( domain.getCustomerId() );
        subscriptionEntity.setCustomerVehicleId( domain.getCustomerVehicleId() );
        subscriptionEntity.setCardId( domain.getCardId() );
        subscriptionEntity.setTicketTypeId( domain.getTicketTypeId() );
        subscriptionEntity.setPriceRuleId( domain.getPriceRuleId() );
        subscriptionEntity.setEffectiveFrom( domain.getEffectiveFrom() );
        subscriptionEntity.setEffectiveTo( domain.getEffectiveTo() );
        subscriptionEntity.setPrice( domain.getPrice() );
        subscriptionEntity.setStatus( domain.getStatus() );
        subscriptionEntity.setApprovedBy( domain.getApprovedBy() );
        subscriptionEntity.setApprovedAt( domain.getApprovedAt() );
        subscriptionEntity.setCardReceiptDate( domain.getCardReceiptDate() );

        return subscriptionEntity;
    }

    @Override
    public Subscription toDomain(SubscriptionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Subscription subscription = new Subscription();

        subscription.setCreatedAt( entity.getCreatedAt() );
        subscription.setCreatedBy( entity.getCreatedBy() );
        subscription.setUpdatedAt( entity.getUpdatedAt() );
        subscription.setUpdatedBy( entity.getUpdatedBy() );
        subscription.setSubscriptionId( entity.getSubscriptionId() );
        subscription.setCustomerId( entity.getCustomerId() );
        subscription.setCustomerVehicleId( entity.getCustomerVehicleId() );
        subscription.setCardId( entity.getCardId() );
        subscription.setTicketTypeId( entity.getTicketTypeId() );
        subscription.setPriceRuleId( entity.getPriceRuleId() );
        subscription.setEffectiveFrom( entity.getEffectiveFrom() );
        subscription.setEffectiveTo( entity.getEffectiveTo() );
        subscription.setPrice( entity.getPrice() );
        subscription.setStatus( entity.getStatus() );
        subscription.setApprovedBy( entity.getApprovedBy() );
        subscription.setApprovedAt( entity.getApprovedAt() );
        subscription.setCardReceiptDate( entity.getCardReceiptDate() );

        return subscription;
    }
}
