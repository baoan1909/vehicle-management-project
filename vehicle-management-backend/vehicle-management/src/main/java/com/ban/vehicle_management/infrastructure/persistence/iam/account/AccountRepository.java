package com.ban.vehicle_management.infrastructure.persistence.iam.account;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
}
