package com.ban.vehicle_management.application.catalog.tickettype.usecase;

import com.ban.vehicle_management.application.catalog.tickettype.port.in.TicketTypePortIn;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.tickettype.policy.TicketTypePolicy;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TicketTypeUsecaseImpl implements TicketTypePortIn {
    private  final TicketTypePortOut ticketTypePortOut;
    private  final TicketTypePolicy ticketTypePolicy = new TicketTypePolicy();

    public TicketTypeUsecaseImpl (TicketTypePortOut ticketTypePortOut){
        this.ticketTypePortOut = ticketTypePortOut;
    }

    @Override
    @Transactional
    public TicketType createTicketType(TicketType ticketType){
        ticketTypePolicy.initialize(ticketType);

        if(ticketTypePortOut.existsActiveByCode(ticketType.getCode())){
            throw new ConflictException("Active ticket type code already exists");
        }

        ticketType.setTicketTypeId(UUID.randomUUID());
        return ticketTypePortOut.save(ticketType);
    }

    @Override
    @Transactional(readOnly = true)
    public  TicketType getTicketTypeById(UUID ticketTypeId){
        return ticketTypePortOut.findById(ticketTypeId)
                .orElseThrow(() -> new NotFoundException("Ticket type not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketType> getTicketTypes(TicketTypeStatus status, String keyword){
        return ticketTypePortOut.findAll(status, normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public TicketType updateTicketType(UUID ticketTypeId, TicketType ticketType){
        TicketType existingTicketType = getTicketTypeById(ticketTypeId);

        if (existingTicketType.getStatus() != TicketTypeStatus.ACTIVE){
            throw new BadRequestException("Only active ticket type can be update");
        }

        boolean codeChanged = !existingTicketType.getCode().equalsIgnoreCase(ticketType.getCode());
        if (codeChanged && (ticketTypePortOut.hasActivePriceRules(ticketTypeId))){
            throw new ConflictException("used ticket type code cannot be changed");
        }

        existingTicketType.setCode(ticketType.getCode());
        existingTicketType.setName(ticketType.getName());
        existingTicketType.setDescription(ticketType.getDescription());

        ticketTypePolicy.initialize(existingTicketType);

        if (ticketTypePortOut.existsActiveByCodeAndTicketTypeIdNot(existingTicketType.getCode(), ticketTypeId)){
            throw new ConflictException("Active ticket type code already exists");
        }

        return ticketTypePortOut.save(existingTicketType);
    }

    @Override
    @Transactional
    public void deleteTicketType(UUID ticketTypeId){
        TicketType existingTicketType = getTicketTypeById(ticketTypeId);
        if (existingTicketType.getStatus() == TicketTypeStatus.INACTIVE){
            return;
        }

        if (ticketTypePortOut.hasActivePriceRules(ticketTypeId)){
            throw new ConflictException("Ticket type is used by active price rules");
        }

        if (ticketTypePortOut.hasBlockingSubcriptions(ticketTypeId)){
            throw new ConflictException("Ticket type is used by active or pending subcriptions");
        }

        ticketTypePolicy.deactivate(existingTicketType);
        ticketTypePortOut.save(existingTicketType);
    }

    @Override
    @Transactional
    public TicketType activateTicketType(UUID ticketTypeId){
        TicketType existingTicketType = getTicketTypeById(ticketTypeId);

        if (existingTicketType.getStatus() == TicketTypeStatus.ACTIVE){
            return existingTicketType;
        }

        ticketTypePolicy.activate(existingTicketType);

        if (ticketTypePortOut.existsActiveByCodeAndTicketTypeIdNot(existingTicketType.getCode(), ticketTypeId)){
            throw new ConflictException("Active ticket type code already exists");
        }

        return ticketTypePortOut.save(existingTicketType);
    }

    private String normalizeKeyword(String keyword){
        return  keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

}
