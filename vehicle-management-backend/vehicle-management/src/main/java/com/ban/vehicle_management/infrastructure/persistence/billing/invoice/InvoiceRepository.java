package com.ban.vehicle_management.infrastructure.persistence.billing.invoice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {
}
