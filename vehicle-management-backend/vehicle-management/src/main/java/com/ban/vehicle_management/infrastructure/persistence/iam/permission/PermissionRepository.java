package com.ban.vehicle_management.infrastructure.persistence.iam.permission;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {
}
