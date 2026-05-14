package com.ban.vehicle_management.infrastructure.persistence.audit.auditlog;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
}
