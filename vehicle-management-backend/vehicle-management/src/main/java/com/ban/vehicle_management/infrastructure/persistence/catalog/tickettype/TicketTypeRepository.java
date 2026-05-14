package com.ban.vehicle_management.infrastructure.persistence.catalog.tickettype;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketTypeRepository extends JpaRepository<TicketTypeEntity, UUID> {
}
