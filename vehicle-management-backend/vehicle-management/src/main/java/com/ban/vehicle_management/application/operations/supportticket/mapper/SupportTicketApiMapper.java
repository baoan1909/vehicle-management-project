package com.ban.vehicle_management.application.operations.supportticket.mapper;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.CreateSupportTicketRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.UpdateSupportTicketRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.response.SupportTicketAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupportTicketApiMapper {

    @Mapping(target = "supportTicketId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "categoryCode", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "resolutionNote", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "closedBy", ignore = true)
    @Mapping(target = "reopenCount", ignore = true)
    @Mapping(target = "lastReopenedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    SupportTicket toDomain(CreateSupportTicketRequest request);

    @Mapping(target = "supportTicketId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "categoryCode", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "resolutionNote", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "closedBy", ignore = true)
    @Mapping(target = "reopenCount", ignore = true)
    @Mapping(target = "lastReopenedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    SupportTicket toDomain(UpdateSupportTicketRequest request);

    SupportTicketAdminResponse toAdminResponse(SupportTicket supportTicket);

    List<SupportTicketAdminResponse> toAdminResponses(List<SupportTicket> supportTickets);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
