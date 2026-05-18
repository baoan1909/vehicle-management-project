package com.ban.vehicle_management.domain.catalog.tickettype.policy;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class TicketTypePolicy {

    public void initialize(TicketType ticketType) {
        requireTicketType(ticketType);
        ticketType.setCode(TextValidationUtils.normalizeCode(ticketType.getCode(), "code", 50));
        ticketType.setName(TextValidationUtils.normalizeRequiredText(ticketType.getName(), "name", 100));
        ticketType.setDescription(TextValidationUtils.normalizeNullableText(ticketType.getDescription(), "description", 0));
        if (ticketType.getIsActive() == null) {
            ticketType.setIsActive(Boolean.TRUE);
        }
        if (ticketType.getDurationDays() != null && ticketType.getDurationDays() <= 0) {
            throw new BadRequestException("durationDays must be greater than zero");
        }
    }

    public void deactivate(TicketType ticketType) {
        requireTicketType(ticketType);
        ticketType.setIsActive(Boolean.FALSE);
    }

    private void requireTicketType(TicketType ticketType) {
        if (ticketType == null) {
            throw new BadRequestException("ticketType must not be null");
        }
    }
}

