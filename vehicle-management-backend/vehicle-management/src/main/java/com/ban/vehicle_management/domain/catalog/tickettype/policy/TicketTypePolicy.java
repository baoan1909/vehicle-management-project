package com.ban.vehicle_management.domain.catalog.tickettype.policy;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class TicketTypePolicy {

    public void initialize(TicketType ticketType) {
        requireTicketType(ticketType);
        ticketType.setCode(TextValidationUtils.normalizeCode(ticketType.getCode(), "code", 50));
        ticketType.setName(TextValidationUtils.normalizeRequiredText(ticketType.getName(), "name", 100));
        ticketType.setDescription(TextValidationUtils.normalizeNullableText(ticketType.getDescription(), "description", 0));
        if (ticketType.getStatus() == null) {
            ticketType.setStatus(TicketTypeStatus.ACTIVE);
        }
        if (ticketType.getDurationDays() != null && ticketType.getDurationDays() <= 0) {
            throw new BadRequestException("durationDays must be greater than zero");
        }
    }

    public void deactivate(TicketType ticketType) {
        requireTicketType(ticketType);
        ticketType.setStatus(TicketTypeStatus.INACTIVE);
    }
    public  void activate(TicketType ticketType){
        requireTicketType(ticketType);
        ticketType.setStatus(TicketTypeStatus.ACTIVE);
        validateState(ticketType);
    }

    private void requireTicketType(TicketType ticketType) {
        if (ticketType == null) {
            throw new BadRequestException("ticketType must not be null");
        }
    }

    public void validateState(TicketType ticketType){
        requireTicketType(ticketType);
        ticketType.setCode(TextValidationUtils.normalizeCode(ticketType.getCode(),"code", 50));
        ticketType.setName(TextValidationUtils.normalizeRequiredText(ticketType.getName(),"name", 150));
        ticketType.setDescription(TextValidationUtils.normalizeNullableText(ticketType.getDescription(),"description",0));
        ticketType.setDurationDays(durationByCode(ticketType.getCode()));
    }

    private int durationByCode(String code) {
        return switch (code) {
            case "DAILY" -> 1;
            case "MONTHLY" -> 30;
            case "QUARTERLY" -> 90;
            case "YEARLY" -> 365;
            case "FREE" -> 180;
            default -> throw new BadRequestException("Unsupported ticket type code");
        };
    }
}

