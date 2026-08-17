package com.ban.vehicle_management.application.accesscontrol.card.mapper;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.CreateCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.UpdateCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.response.CardAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CardApiMapper {

    @Mapping(target = "cardId", ignore = true)
    @Mapping(target = "cardNumber", ignore = true)
    @Mapping(target = "uid", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "issuedAt", ignore = true)
    @Mapping(target = "statusBeforeBlocked", ignore = true)
    @Mapping(target = "blockedAt", ignore = true)
    @Mapping(target = "blockedBy", ignore = true)
    @Mapping(target = "blockedReason", ignore = true)
    @Mapping(target = "retiredAt", ignore = true)
    @Mapping(target = "retiredBy", ignore = true)
    @Mapping(target = "retiredReason", ignore = true)
    @Mapping(target = "recoveredAt", ignore = true)
    @Mapping(target = "recoveredBy", ignore = true)
    @Mapping(target = "recoveryNote", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Card toDomain(CreateCardRequest request);

    @Mapping(target = "cardId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "issuedAt", ignore = true)
    @Mapping(target = "statusBeforeBlocked", ignore = true)
    @Mapping(target = "blockedAt", ignore = true)
    @Mapping(target = "blockedBy", ignore = true)
    @Mapping(target = "blockedReason", ignore = true)
    @Mapping(target = "retiredAt", ignore = true)
    @Mapping(target = "retiredBy", ignore = true)
    @Mapping(target = "retiredReason", ignore = true)
    @Mapping(target = "recoveredAt", ignore = true)
    @Mapping(target = "recoveredBy", ignore = true)
    @Mapping(target = "recoveryNote", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Card toDomain(UpdateCardRequest request);

    @Mapping(target = "registeredVehicleTypeId", ignore = true)
    @Mapping(target = "registeredVehicleTypeCode", ignore = true)
    @Mapping(target = "registeredVehicleTypeName", ignore = true)
    @Mapping(target = "subscriptionId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "customerVehicleId", ignore = true)
    @Mapping(target = "ticketTypeId", ignore = true)
    @Mapping(target = "ticketTypeCode", ignore = true)
    @Mapping(target = "ticketTypeName", ignore = true)
    @Mapping(target = "requestedEffectiveFrom", ignore = true)
    @Mapping(target = "effectiveFrom", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(target = "subscriptionPrice", ignore = true)
    @Mapping(target = "subscriptionStatus", ignore = true)
    @Mapping(target = "cardReceiptDate", ignore = true)
    @Mapping(target = "licensePlate", ignore = true)
    @Mapping(target = "vehicleBrand", ignore = true)
    @Mapping(target = "vehicleColor", ignore = true)
    @Mapping(target = "customerCode", ignore = true)
    @Mapping(target = "customerType", ignore = true)
    @Mapping(target = "customerStatus", ignore = true)
    @Mapping(target = "customerApprovalStatus", ignore = true)
    @Mapping(target = "customerEmail", ignore = true)
    @Mapping(target = "customerFullName", ignore = true)
    @Mapping(target = "customerPhoneNumber", ignore = true)
    CardAdminResponse toAdminResponse(Card card);

    List<CardAdminResponse> toAdminResponses(List<Card> cards);

    default String map(Instant value) {
        return DateTimeUtils.formatInstant(value, DateTimeUtils.VIETNAM_ZONE);
    }
}
