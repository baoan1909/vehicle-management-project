package com.ban.vehicle_management.infrastructure.persistence.iam.account;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, UUID> {
}
