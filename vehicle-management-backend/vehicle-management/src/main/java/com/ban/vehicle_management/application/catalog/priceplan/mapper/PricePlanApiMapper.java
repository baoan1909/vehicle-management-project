package com.ban.vehicle_management.application.catalog.priceplan.mapper;

import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.request.CreatePricePlanRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.request.UpdatePricePlanRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.response.PricePlanAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.response.PricePlanUserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PricePlanApiMapper {

    @Mapping(target = "pricePlanId", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    PricePlan toDomain(CreatePricePlanRequest request);

    @Mapping(target = "pricePlanId", ignore = true)
    @Mapping(target = "appliesTo", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    PricePlan toDomain(UpdatePricePlanRequest request);

    PricePlanAdminResponse toAdminResponse(PricePlan pricePlan);

    List<PricePlanUserResponse> toUserResponses(List<PricePlan> items);

    List<PricePlanAdminResponse> toAdminResponses(List<PricePlan> pricePlans);

}
