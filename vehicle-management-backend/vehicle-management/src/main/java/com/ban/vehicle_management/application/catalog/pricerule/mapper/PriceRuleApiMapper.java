package com.ban.vehicle_management.application.catalog.pricerule.mapper;

import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.request.CreatePriceRuleRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.request.UpdatePriceRuleRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.response.PriceRuleAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PriceRuleApiMapper {

    @Mapping(target = "priceRuleId", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    PriceRule toDomain(CreatePriceRuleRequest request);

    @Mapping(target = "priceRuleId", ignore = true)
    @Mapping(target = "pricePlanId", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    PriceRule toDomain(UpdatePriceRuleRequest request);

    PriceRuleAdminResponse toAdminResponse(PriceRule priceRule);

    List<PriceRuleAdminResponse> toAdminResponses(List<PriceRule> priceRules);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}