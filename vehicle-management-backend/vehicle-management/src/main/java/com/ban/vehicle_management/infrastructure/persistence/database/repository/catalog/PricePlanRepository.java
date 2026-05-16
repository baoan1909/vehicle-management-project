package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PricePlanEntity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricePlanRepository extends JpaRepository<PricePlanEntity, UUID> {
}


