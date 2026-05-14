package com.ban.vehicle_management.infrastructure.persistence.catalog.pricerule;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRuleRepository extends JpaRepository<PriceRuleEntity, UUID> {
}
