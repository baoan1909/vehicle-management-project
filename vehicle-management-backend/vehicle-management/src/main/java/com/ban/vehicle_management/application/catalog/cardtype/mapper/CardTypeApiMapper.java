package com.ban.vehicle_management.application.catalog.cardtype.mapper;

import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.request.CreateCardTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.request.UpdateCardTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.response.CardTypeAdminResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CardTypeApiMapper {

    @Mapping(target = "cardTypeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    CardType toDomain(CreateCardTypeRequest request);

    @Mapping(target = "cardTypeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    CardType toDomain(UpdateCardTypeRequest request);

    CardTypeAdminResponse toAdminResponse(CardType cardType);

    List<CardTypeAdminResponse> toAdminResponses(List<CardType> cardTypes);
}

