package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.CardTypeEntity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTypeRepository extends JpaRepository<CardTypeEntity, UUID> {

    List<CardTypeEntity> findAllByOrderByCodeAsc();

    boolean existsByCode(String code);

    boolean existsByCodeAndCardTypeIdNot(String code, UUID cardTypeId);
}


