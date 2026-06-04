package com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.response;

import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TicketTypeAdminResponse {
    private UUID ticketTypeId;
    private String code;
    private String name;
    private String description;
    private Integer durationDays;
    private TicketTypeStatus status;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private  UUID updatedBy;
}
