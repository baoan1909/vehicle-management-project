package com.ban.vehicle_management.infrastructure.persistence.database.repository.audit;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.audit.AuditLogEntity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
}


