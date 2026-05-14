package com.ban.vehicle_management.infrastructure.persistence.iam.role;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
}
