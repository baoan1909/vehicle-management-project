package com.ban.vehicle_management.domain.catalog.tickettype.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketType extends AuditableDomainModel {

    private UUID ticketTypeId;
    private UUID organizationId;
    private String code;
    private String name;
    private String description;
    private Integer durationDays;
    private TicketTypeStatus status;
}

