package com.ban.vehicle_management.infrastructure.persistence.accesscontrol.card;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<CardEntity, UUID> {
}
