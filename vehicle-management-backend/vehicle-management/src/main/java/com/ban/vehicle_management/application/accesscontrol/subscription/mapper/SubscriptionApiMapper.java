package com.ban.vehicle_management.application.accesscontrol.subscription.mapper;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request.CreateSubscriptionAdminRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request.CreateSubscriptionRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request.UpdateSubscriptionRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response.SubscriptionAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubscriptionApiMapper {

    @Mapping(target = "subscriptionId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "cardId", ignore = true)
    @Mapping(target = "priceRuleId", ignore = true)
    @Mapping(target = "effectiveFrom", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "rejectedBy", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "cardReceiptDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Subscription toDomain(CreateSubscriptionRequest request);

    @Mapping(target = "subscriptionId", ignore = true)
    @Mapping(target = "cardId", ignore = true)
    @Mapping(target = "priceRuleId", ignore = true)
    @Mapping(target = "effectiveFrom", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "rejectedBy", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "cardReceiptDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Subscription toDomain(CreateSubscriptionAdminRequest request);

    @Mapping(target = "subscriptionId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "cardId", ignore = true)
    @Mapping(target = "priceRuleId", ignore = true)
    @Mapping(target = "effectiveFrom", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "rejectedBy", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "cardReceiptDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Subscription toDomain(UpdateSubscriptionRequest request);

    SubscriptionAdminResponse toAdminResponse(Subscription subscription);

    List<SubscriptionAdminResponse> toAdminResponses(List<Subscription> subscriptions);

    default String map(Instant value) {
        return DateTimeUtils.formatInstant(value, DateTimeUtils.VIETNAM_ZONE);
    }
}