package com.ban.vehicle_management.application.accesscontrol.card.port.out;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardNumberSeries;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardPortOut {

    Card save(Card card);

    List<Card> saveAll(List<Card> cards);

    Optional<Card> findById(UUID cardId);

    Optional<Card> findByIdForUpdate(UUID cardId);

    Optional<Card> findByUid(String uid);

    Optional<Card> findByUidForUpdate(String uid);

    List<Card> findAll(CardStatus status, UUID cardTypeId, String keyword);

    boolean existsByCardNumber(String cardNumber);

    boolean existsByUid(String uid);

    boolean existsByCardNumberAndCardIdNot(String cardNumber, UUID cardId);

    boolean existsByUidAndCardIdNot(String uid, UUID cardId);

    long nextCardNumberSequence(CardNumberSeries series);

    boolean hasOperationalHistory(UUID cardId);

    boolean hasActiveUsage(UUID cardId);

    boolean canRestoreBlockedStatus(UUID cardId, CardStatus statusBeforeBlocked);

    boolean canRecoverLostCard(UUID cardId);

    boolean hasOpenLostCardReport(UUID cardId);

    Optional<Card> findFirstAvailableRegistered();
}

