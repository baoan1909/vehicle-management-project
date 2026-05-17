package com.ban.vehicle_management.application.catalog.cardtype.port.out;

import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardTypePortOut {

    CardType save(CardType cardType);

    Optional<CardType> findById(UUID cardTypeId);

    List<CardType> findAll();

    boolean existsByCode(String code);

    boolean existsByCodeAndCardTypeIdNot(String code, UUID cardTypeId);
}

