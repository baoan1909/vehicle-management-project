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
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardNumberSeries;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class CardPersistenceAdapter implements CardPortOut {

    private final CardRepository cardRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LostCardReportRepository lostCardReportRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final CardPersistenceMapper cardPersistenceMapper;
    private final EntityManager entityManager;
    private static final String CARD_TYPE_REGISTERED = "REGISTERED";

    public CardPersistenceAdapter(
            CardRepository cardRepository,
            SubscriptionRepository subscriptionRepository,
            LostCardReportRepository lostCardReportRepository,
            ParkingSessionRepository parkingSessionRepository,
            CardPersistenceMapper cardPersistenceMapper,
            EntityManager entityManager
    ) {
        this.cardRepository = cardRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.lostCardReportRepository = lostCardReportRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.cardPersistenceMapper = cardPersistenceMapper;
        this.entityManager = entityManager;
    }

    @Override
    public Card save(Card card) {
        CardEntity savedCardEntity = cardRepository.saveAndFlush(cardPersistenceMapper.toEntity(card));
        return cardPersistenceMapper.toDomain(savedCardEntity);
    }

    @Override
    public List<Card> saveAll(List<Card> cards) {
        List<CardEntity> savedCardEntities = cardRepository.saveAll(cardPersistenceMapper.toEntities(cards));
        cardRepository.flush();
        return cardPersistenceMapper.toDomains(savedCardEntities);
    }

    @Override
    public Optional<Card> findById(UUID cardId) {
        return cardRepository.findById(cardId)
                .map(cardPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Card> findByIdForUpdate(UUID cardId) {
        return cardRepository.findByIdForUpdate(cardId)
                .map(cardPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Card> findByUid(String uid) {
        return cardRepository.findByUid(uid)
                .map(cardPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Card> findByUidForUpdate(String uid) {
        return cardRepository.findByUidForUpdate(uid)
                .map(cardPersistenceMapper::toDomain);
    }

    @Override
    public List<Card> findAll(CardStatus status, UUID cardTypeId, String keyword) {
        return cardRepository.findAll(CardSpecifications.withFilters(status, cardTypeId, keyword)).stream()
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
    public long nextCardNumberSequence(CardNumberSeries series) {
        String sequenceName = switch (series) {
            case REGISTERED -> "access_control.registered_card_number_seq";
            case VISITOR -> "access_control.visitor_card_number_seq";
        };

        return ((Number) entityManager
                .createNativeQuery("SELECT nextval('" + sequenceName + "')")
                .getSingleResult())
                .longValue();
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
                List.of(
                        SubscriptionStatus.PENDING_PAYMENT,
                        SubscriptionStatus.PENDING_CARD,
                        SubscriptionStatus.ACTIVE
                )
        )
                || lostCardReportRepository.existsByCardIdAndStatus(cardId, LostCardReportStatus.OPEN)
                || parkingSessionRepository.existsByCardIdAndStatusIn(
                        cardId,
                        List.of(ParkingSessionStatus.OPEN, ParkingSessionStatus.LOST_CARD)
                );
    }

    @Override
    public boolean canRestoreBlockedStatus(UUID cardId, CardStatus statusBeforeBlocked) {
        if (statusBeforeBlocked == null) {
            return false;
        }

        return switch (statusBeforeBlocked) {
            case AVAILABLE -> !hasActiveUsage(cardId);
            case RESERVED -> subscriptionRepository.existsByCardIdAndStatusIn(
                    cardId,
                    List.of(SubscriptionStatus.PENDING_PAYMENT, SubscriptionStatus.PENDING_CARD)
            );
            case ASSIGNED -> subscriptionRepository.existsByCardIdAndStatusIn(
                    cardId,
                    List.of(SubscriptionStatus.ACTIVE)
            )
                    && !parkingSessionRepository.existsByCardIdAndStatus(cardId, ParkingSessionStatus.OPEN);
            case IN_USE -> parkingSessionRepository.existsByCardIdAndStatus(cardId, ParkingSessionStatus.OPEN);
            case LOST, BLOCKED, RETIRED -> false;
        };
    }

    @Override
    public boolean canRecoverLostCard(UUID cardId) {
        return lostCardReportRepository.existsByCardIdAndStatus(cardId, LostCardReportStatus.RESOLVED)
                && !hasActiveUsage(cardId);
    }

    @Override
    public boolean hasOpenLostCardReport(UUID cardId) {
        return lostCardReportRepository.existsByCardIdAndStatus(cardId, LostCardReportStatus.OPEN);
    }

    @Override
    public Optional<Card> findFirstAvailableRegistered() {
        return cardRepository.findAvailableByCardTypeCodeForUpdate(
                        CardStatus.AVAILABLE,
                        CARD_TYPE_REGISTERED
                )
                .stream()
                .findFirst()
                .map(cardPersistenceMapper::toDomain);
    }
}

