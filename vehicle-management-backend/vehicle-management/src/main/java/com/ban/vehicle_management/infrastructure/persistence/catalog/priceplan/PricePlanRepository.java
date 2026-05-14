package com.ban.vehicle_management.infrastructure.persistence.catalog.priceplan;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricePlanRepository extends JpaRepository<PricePlanEntity, UUID> {
}
