package com.ban.vehicle_management.domain.catalog.tickettype.policy;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.shared.exception.BadRequestException;

public class TicketTypePolicy {

    public void initialize(TicketType ticketType) {
        requireTicketType(ticketType);
        ticketType.setCode(normalizeRequired(ticketType.getCode(), "code"));
        ticketType.setName(normalizeRequired(ticketType.getName(), "name"));
        ticketType.setDescription(normalizeNullable(ticketType.getDescription()));
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

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = normalizeNullable(value);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}

