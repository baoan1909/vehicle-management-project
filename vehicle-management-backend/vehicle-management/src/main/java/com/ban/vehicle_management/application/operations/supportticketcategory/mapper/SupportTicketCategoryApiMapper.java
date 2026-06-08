package com.ban.vehicle_management.application.operations.supportticketcategory.mapper;

import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request.CreateSupportTicketCategoryRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request.UpdateSupportTicketCategoryRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.response.SupportTicketCategoryAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupportTicketCategoryApiMapper {

    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    SupportTicketCategory toDomain(CreateSupportTicketCategoryRequest request);

    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    SupportTicketCategory toDomain(UpdateSupportTicketCategoryRequest request);

    SupportTicketCategoryAdminResponse toAdminResponse(SupportTicketCategory category);

    List<SupportTicketCategoryAdminResponse> toAdminResponses(List<SupportTicketCategory> categories);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}