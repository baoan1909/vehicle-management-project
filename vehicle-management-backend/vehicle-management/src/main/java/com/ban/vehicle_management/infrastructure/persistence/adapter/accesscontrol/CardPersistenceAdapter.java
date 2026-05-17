package com.ban.vehicle_management.infrastructure.persistence.adapter.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.infrastructure.mapper.accesscontrol.CardPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.CardRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.accesscontrol.CardSpecifications;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.shared.enumeration.CardStatus;
import com.ban.vehicle_management.shared.enumeration.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.SubscriptionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CardPersistenceAdapter implements CardPortOut {

    private final CardRepository cardRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LostCardReportRepository lostCardReportRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final CardPersistenceMapper cardPersistenceMapper;

    public CardPersistenceAdapter(
            CardRepository cardRepository,
            SubscriptionRepository subscriptionRepository,
            LostCardReportRepository lostCardReportRepository,
            ParkingSessionRepository parkingSessionRepository,
            CardPersistenceMapper cardPersistenceMapper
    ) {
        this.cardRepository = cardRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.lostCardReportRepository = lostCardReportRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.cardPersistenceMapper = cardPersistenceMapper;
    }

    @Override
    public Card save(Card card) {
        CardEntity savedCardEntity = cardRepository.save(cardPersistenceMapper.toEntity(card));
        return cardPersistenceMapper.toDomain(savedCardEntity);
    }

    @Override
    public Optional<Card> findById(UUID cardId) {
        return cardRepository.findById(cardId)
                .map(cardPersistenceMapper::toDomain);
    }

    @Override
    public List<Card> findAll(CardStatus status, UUID cardTypeId, UUID vehicleTypeId, String keyword) {
        return cardRepository.findAll(CardSpecifications.withFilters(status, cardTypeId, vehicleTypeId, keyword)).stream()
                .map(cardPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCardNumber(String cardNumber) {
        return cardRepository.existsByCardNumber(cardNumber);
    }

    @Override
    public boolean existsByUid(String uid) {
        return cardRepository.existsByUid(uid);
    }

    @Override
    public boolean existsByCardNumberAndCardIdNot(String cardNumber, UUID cardId) {
        return cardRepository.existsByCardNumberAndCardIdNot(cardNumber, cardId);
    }

    @Override
    public boolean existsByUidAndCardIdNot(String uid, UUID cardId) {
        return cardRepository.existsByUidAndCardIdNot(uid, cardId);
    }

    @Override
    public boolean hasOperationalHistory(UUID cardId) {
        return subscriptionRepository.existsByCardId(cardId)
                || lostCardReportRepository.existsByCardId(cardId)
                || parkingSessionRepository.existsByCardId(cardId);
    }

    @Override
    public boolean hasActiveUsage(UUID cardId) {
        return subscriptionRepository.existsByCardIdAndStatusIn(
                cardId,
                List.of(SubscriptionStatus.PENDING, SubscriptionStatus.ACTIVE)
        )
                || lostCardReportRepository.existsByCardIdAndStatus(cardId, LostCardReportStatus.OPEN)
                || parkingSessionRepository.existsByCardIdAndStatusIn(
                        cardId,
                        List.of(ParkingSessionStatus.OPEN, ParkingSessionStatus.LOST_CARD)
                );
    }
}

