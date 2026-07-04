package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.tickettype.mapper.TicketTypeApiMapper;
import com.ban.vehicle_management.application.catalog.tickettype.port.in.TicketTypePortIn;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.request.CreateTicketTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.request.TicketTypeFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.request.UpdateTicketTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.response.TicketTypeAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/ticket-types")
public class TicketTypeController {
    private final TicketTypePortIn ticketTypePortIn;
    private final TicketTypeApiMapper ticketTypeApiMapper;

    public TicketTypeController(TicketTypePortIn ticketTypePortIn, TicketTypeApiMapper ticketTypeApiMapper){
        this.ticketTypePortIn = ticketTypePortIn;
        this.ticketTypeApiMapper = ticketTypeApiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('TICKET_TYPE_CREATE_ALL')")
    public ResponseEntity<ApiResponse<TicketTypeAdminResponse>> createTicketType(@RequestBody CreateTicketTypeRequest request){
        TicketType createdTicketType = ticketTypePortIn.createTicketType(ticketTypeApiMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Ticket type created successfully", ticketTypeApiMapper.toAdminResponse(createdTicketType)));
    }

    @GetMapping("/{ticketTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('TICKET_TYPE_READ_ALL')")
    public ResponseEntity<ApiResponse<TicketTypeAdminResponse>> getTicketTypeById(@PathVariable UUID ticketTypeId){
        TicketType ticketType =ticketTypePortIn.getTicketTypeById(ticketTypeId);
        return  ResponseEntity.ok(ApiResponse.ok("Fetched ticket type successfully", ticketTypeApiMapper.toAdminResponse(ticketType)));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('TICKET_TYPE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<TicketTypeAdminResponse>>> getTicketTypes(@ModelAttribute TicketTypeFilterRequest request){
        List<TicketType> ticketTypes = ticketTypePortIn.getTicketTypes(request.status(), request.keyword());
        return  ResponseEntity.ok(ApiResponse.ok("Fetched ticket types successfully", ticketTypeApiMapper.toAdminResponses(ticketTypes)));
    }

    @PutMapping("/{ticketTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('TICKET_TYPE_UPDATE_ALL')")
    public  ResponseEntity<ApiResponse<TicketTypeAdminResponse>> updateTicketType(
            @PathVariable UUID ticketTypeId,
            @RequestBody UpdateTicketTypeRequest request
            ){
        TicketType updatedTicketType = ticketTypePortIn.updateTicketType(ticketTypeId, ticketTypeApiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok("Ticket type updated successfully", ticketTypeApiMapper.toAdminResponse(updatedTicketType)));
    }

    @DeleteMapping("/{ticketTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('TICKET_TYPE_DELETE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteTicketType(@PathVariable UUID ticketTypeId) {
        ticketTypePortIn.deleteTicketType(ticketTypeId);
        return ResponseEntity.ok(ApiResponse.ok("Ticket type deactivated successfully"));
    }

    @PatchMapping("/{ticketTypeId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('TICKET_TYPE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<TicketTypeAdminResponse>> activateTicketType(@PathVariable UUID ticketTypeId) {
        TicketType ticketType = ticketTypePortIn.activateTicketType(ticketTypeId);
        return ResponseEntity.ok(ApiResponse.ok("Ticket type activated successfully", ticketTypeApiMapper.toAdminResponse(ticketType)));
    }
}
