package com.ban.vehicle_management.application.catalog.tickettype.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.catalog.tickettype.port.in.TicketTypePortIn;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.tickettype.policy.TicketTypePolicy;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TicketTypeUsecaseImpl implements TicketTypePortIn {
    private static final String TICKET_TYPE_CREATE_ALL = "TICKET_TYPE_CREATE_ALL";
    private static final String TICKET_TYPE_READ_ALL = "TICKET_TYPE_READ_ALL";
    private static final String TICKET_TYPE_UPDATE_ALL = "TICKET_TYPE_UPDATE_ALL";
    private static final String TICKET_TYPE_DELETE_ALL = "TICKET_TYPE_DELETE_ALL";

    private  final CurrentAccountPortIn currentAccountPortIn;
    private  final TicketTypePortOut ticketTypePortOut;
    private final NotificationPortIn notificationPortIn;
    private  final TicketTypePolicy ticketTypePolicy = new TicketTypePolicy();

    public TicketTypeUsecaseImpl (
            CurrentAccountPortIn currentAccountPortIn,
            TicketTypePortOut ticketTypePortOut,
            NotificationPortIn notificationPortIn
    ){
        this.currentAccountPortIn = currentAccountPortIn;
        this.ticketTypePortOut = ticketTypePortOut;
        this.notificationPortIn = notificationPortIn;
    }

    @Override
    @Transactional
    public TicketType createTicketType(TicketType ticketType){
        currentAccountPortIn.requirePermission(TICKET_TYPE_CREATE_ALL);
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
        currentAccountPortIn.requirePermission(TICKET_TYPE_READ_ALL);
        return findExistingTicketType(ticketTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketType> getTicketTypes(TicketTypeStatus status, String keyword){
        currentAccountPortIn.requirePermission(TICKET_TYPE_READ_ALL);
        return ticketTypePortOut.findAll(status, normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public TicketType updateTicketType(UUID ticketTypeId, TicketType ticketType){
        currentAccountPortIn.requirePermission(TICKET_TYPE_UPDATE_ALL);
        TicketType existingTicketType = findExistingTicketType(ticketTypeId);

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

        TicketType savedTicketType = ticketTypePortOut.save(existingTicketType);
        notifyTicketTypeChanged(savedTicketType, "Loại vé được cập nhật");
        return savedTicketType;
    }

    @Override
    @Transactional
    public void deleteTicketType(UUID ticketTypeId){
        currentAccountPortIn.requirePermission(TICKET_TYPE_DELETE_ALL);
        TicketType existingTicketType = findExistingTicketType(ticketTypeId);
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
        TicketType savedTicketType = ticketTypePortOut.save(existingTicketType);
        notifyTicketTypeChanged(savedTicketType, "Loại vé ngừng áp dụng");
    }

    @Override
    @Transactional
    public TicketType activateTicketType(UUID ticketTypeId){
        currentAccountPortIn.requirePermission(TICKET_TYPE_UPDATE_ALL);
        TicketType existingTicketType = findExistingTicketType(ticketTypeId);

        if (existingTicketType.getStatus() == TicketTypeStatus.ACTIVE){
            return existingTicketType;
        }

        ticketTypePolicy.activate(existingTicketType);

        if (ticketTypePortOut.existsActiveByCodeAndTicketTypeIdNot(existingTicketType.getCode(), ticketTypeId)){
            throw new ConflictException("Active ticket type code already exists");
        }

        TicketType savedTicketType = ticketTypePortOut.save(existingTicketType);
        notifyTicketTypeChanged(savedTicketType, "Loại vé được kích hoạt");
        return savedTicketType;
    }

    private String normalizeKeyword(String keyword){
        return  keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private TicketType findExistingTicketType(UUID ticketTypeId) {
        return ticketTypePortOut.findById(ticketTypeId)
                .orElseThrow(() -> new NotFoundException("Ticket type not found"));
    }

    private void notifyTicketTypeChanged(TicketType ticketType, String title) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                true,
                null,
                null,
                null,
                NotificationType.TICKET_TYPE_CHANGED,
                title,
                "Loại vé " + ticketType.getName() + " vừa có thay đổi.",
                null,
                "catalog",
                "ticket_types",
                ticketType.getTicketTypeId()
        ));
    }

}
