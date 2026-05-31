package com.ban.vehicle_management.application.accesscontrol.card.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardPortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.ChangeCardStatusPortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.card.policy.CardPolicy;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardUseCaseImpl implements CardPortIn, ChangeCardStatusPortIn {

    private final CardPortOut cardPort;
    private final CardTypePortOut cardTypePort;
    private final VehicleTypePortOut vehicleTypePort;
    private final CardPolicy cardPolicy = new CardPolicy();

    public CardUseCaseImpl(
            CardPortOut cardPort,
            CardTypePortOut cardTypePort,
            VehicleTypePortOut vehicleTypePort
    ) {
        this.cardPort = cardPort;
        this.cardTypePort = cardTypePort;
        this.vehicleTypePort = vehicleTypePort;
    }

    @Override
    @Transactional
    public Card createCard(Card card) {
        cardPolicy.initializeNewCard(card);
        validateCardTypeExists(card.getCardTypeId());
        validateVehicleTypeExists(card.getVehicleTypeId());

        if (cardPort.existsByCardNumber(card.getCardNumber())) {
            throw new ConflictException("Card number already exists");
        }
        if (cardPort.existsByUid(card.getUid())) {
            throw new ConflictException("Card uid already exists");
        }

        card.setCardId(UUID.randomUUID());
        return cardPort.save(card);
    }

    @Override
    @Transactional(readOnly = true)
    public Card getCardById(UUID cardId) {
        return cardPort.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> getCards(CardStatus status, UUID cardTypeId, UUID vehicleTypeId, String keyword) {
        return cardPort.findAll(status, cardTypeId, vehicleTypeId, cardPolicy.normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public Card updateCard(UUID cardId, Card card) {
        Card existingCard = getCardById(cardId);

        if (existingCard.getStatus() == CardStatus.IN_USE) {
            throw new BadRequestException("Card in use cannot be updated");
        }

        if (cardPolicy.hasCoreIdentifierChanged(existingCard, card) && cardPort.hasOperationalHistory(cardId)) {
            throw new BadRequestException("Card number and uid cannot be changed after the card has been used in operational flow");
        }

        existingCard.setCardNumber(card.getCardNumber());
        existingCard.setUid(card.getUid());
        existingCard.setCardTypeId(card.getCardTypeId());
        existingCard.setVehicleTypeId(card.getVehicleTypeId());

        cardPolicy.validateMaintenance(existingCard);
        validateCardTypeExists(existingCard.getCardTypeId());
        validateVehicleTypeExists(existingCard.getVehicleTypeId());

        if (cardPort.existsByCardNumberAndCardIdNot(existingCard.getCardNumber(), cardId)) {
            throw new ConflictException("Card number already exists");
        }
        if (cardPort.existsByUidAndCardIdNot(existingCard.getUid(), cardId)) {
            throw new ConflictException("Card uid already exists");
        }

        return cardPort.save(existingCard);
    }

    @Override
    @Transactional
    public void deleteCard(UUID cardId) {
        Card existingCard = getCardById(cardId);
        if (existingCard.getStatus() == CardStatus.RETIRED) {
            return;
        }
        if (cardPort.hasActiveUsage(cardId)) {
            throw new BadRequestException("Card is currently used in active business flow and cannot be retired");
        }

        cardPolicy.retire(existingCard);
        cardPort.save(existingCard);
    }

    @Override
    @Transactional
    public Card changeCardStatus(UUID cardId, CardStatus status, String blockedReason) {
        Card existingCard = getCardById(cardId);
        if (status == null) {
            throw new BadRequestException("status must not be null");
        }

        switch (status) {
            case BLOCKED -> cardPolicy.block(existingCard, Instant.now(), blockedReason);
            case AVAILABLE -> cardPolicy.unblock(existingCard);
            case LOST -> cardPolicy.markLost(existingCard);
            case DAMAGED -> cardPolicy.markDamaged(existingCard);
            case RETIRED -> {
                if (cardPort.hasActiveUsage(cardId)) {
                    throw new BadRequestException("Card is currently used in active business flow and cannot be retired");
                }
                cardPolicy.retire(existingCard);
            }
            default -> throw new BadRequestException("Unsupported card status transition");
        }

        return cardPort.save(existingCard);
    }

    private void validateCardTypeExists(UUID cardTypeId) {
        if (cardTypePort.findById(cardTypeId).isEmpty()) {
            throw new BadRequestException("Card type does not exist");
        }
    }

    private void validateVehicleTypeExists(UUID vehicleTypeId) {
        if (vehicleTypeId != null && vehicleTypePort.findById(vehicleTypeId).isEmpty()) {
            throw new BadRequestException("Vehicle type does not exist");
        }
    }

}

