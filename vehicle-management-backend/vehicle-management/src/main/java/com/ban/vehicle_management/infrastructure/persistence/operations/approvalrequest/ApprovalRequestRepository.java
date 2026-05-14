package com.ban.vehicle_management.infrastructure.persistence.operations.approvalrequest;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, UUID> {
}
