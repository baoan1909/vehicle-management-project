package com.ban.vehicle_management.application.accesscontrol.card.port.out;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardPortOut {

    Card save(Card card);

    Optional<Card> findById(UUID cardId);

    List<Card> findAll(CardStatus status, UUID cardTypeId, UUID vehicleTypeId, String keyword);

    boolean existsByCardNumber(String cardNumber);

    boolean existsByUid(String uid);

    boolean existsByCardNumberAndCardIdNot(String cardNumber, UUID cardId);

    boolean existsByUidAndCardIdNot(String uid, UUID cardId);

    boolean hasOperationalHistory(UUID cardId);

    boolean hasActiveUsage(UUID cardId);
}

