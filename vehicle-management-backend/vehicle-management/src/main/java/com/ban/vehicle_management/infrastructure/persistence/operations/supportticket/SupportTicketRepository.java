package com.ban.vehicle_management.infrastructure.persistence.operations.supportticket;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, UUID> {
}
