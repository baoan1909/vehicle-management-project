package com.ban.vehicle_management.application.catalog.tickettype.mapper;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.request.CreateTicketTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.request.UpdateTicketTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.response.TicketTypeAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TicketTypeApiMapper {
    @Mapping(target = "ticketTypeId", ignore = true)
    @Mapping(target = "durationDays", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    TicketType toDomain (CreateTicketTypeRequest request);

    @Mapping(target = "ticketTypeId", ignore = true)
    @Mapping(target = "durationDays", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    TicketType toDomain (UpdateTicketTypeRequest request);

    TicketTypeAdminResponse toAdminResponse(TicketType ticketType);

    List<TicketTypeAdminResponse> toAdminResponses(List<TicketType> ticketTypes);

    default  String map(Instant instant){
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
