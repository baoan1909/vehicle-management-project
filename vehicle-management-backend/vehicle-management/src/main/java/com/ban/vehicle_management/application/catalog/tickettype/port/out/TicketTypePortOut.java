package com.ban.vehicle_management.application.catalog.tickettype.port.out;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.tickettype.policy.TicketTypePolicy;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TicketTypePortOut {

    TicketType save(TicketType ticketType);

    Optional<TicketType> findById(UUID ticketTypeId);

    List<TicketType> findAll(TicketTypeStatus status, String keyword);

    List<TicketType> findAll(TicketTypeStatus status, String keyword, Set<UUID> organizationIds);

    boolean existsActiveByCodeInOrganization(String code, UUID organizationId);

    boolean existsActiveByCodeInOrganizationExcludingId(String code, UUID organizationId, UUID ticketTypeId);

    boolean existsActiveByCode(String code);

    boolean existsActiveByCodeAndTicketTypeIdNot(String code, UUID ticketTypeId);

    boolean hasActivePriceRules(UUID ticketTypeId);

    boolean hasBlockingSubcriptions(UUID ticketTypeId);
}
