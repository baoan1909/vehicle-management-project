package com.ban.vehicle_management.infrastructure.persistence.catalog.cardtype;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTypeRepository extends JpaRepository<CardTypeEntity, UUID> {
}
