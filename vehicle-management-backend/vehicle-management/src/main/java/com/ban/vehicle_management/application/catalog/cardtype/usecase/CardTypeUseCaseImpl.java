package com.ban.vehicle_management.application.catalog.cardtype.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.catalog.cardtype.port.in.CardTypePortIn;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.domain.catalog.cardtype.policy.CardTypePolicy;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardTypeUseCaseImpl implements CardTypePortIn {

    private static final String CARD_TYPE_CREATE_ALL = "CARD_TYPE_CREATE_ALL";
    private static final String CARD_TYPE_READ_ALL = "CARD_TYPE_READ_ALL";
    private static final String CARD_TYPE_UPDATE_ALL = "CARD_TYPE_UPDATE_ALL";
    private static final String CARD_TYPE_DELETE_ALL = "CARD_TYPE_DELETE_ALL";
    private static final String PARKING_SESSION_CHECK_IN_ALL = "PARKING_SESSION_CHECK_IN_ALL";
    private static final String PARKING_SESSION_CHECK_OUT_ALL = "PARKING_SESSION_CHECK_OUT_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final CardTypePortOut cardTypePort;
    private final CardTypePolicy cardTypePolicy = new CardTypePolicy();

    public CardTypeUseCaseImpl(CurrentAccountPortIn currentAccountPortIn, CardTypePortOut cardTypePort) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.cardTypePort = cardTypePort;
    }

    @Override
    @Transactional
    public CardType createCardType(CardType cardType) {
        currentAccountPortIn.requirePermission(CARD_TYPE_CREATE_ALL);
        cardTypePolicy.initialize(cardType);

        if (cardTypePort.existsByCode(cardType.getCode())) {
            throw new ConflictException("Card type code already exists");
        }

        cardType.setCardTypeId(UUID.randomUUID());
        return cardTypePort.save(cardType);
    }

    @Override
    @Transactional
    public CardType updateCardType(UUID cardTypeId, CardType cardType) {
        currentAccountPortIn.requirePermission(CARD_TYPE_UPDATE_ALL);
        CardType existingCardType = findExistingCardType(cardTypeId);

        existingCardType.setCode(cardType.getCode());
        existingCardType.setName(cardType.getName());
        existingCardType.setDescription(cardType.getDescription());
        if (cardType.getIsReturnRequired() != null) {
            existingCardType.setIsReturnRequired(cardType.getIsReturnRequired());
        }
        if (cardType.getIsActive() != null) {
            existingCardType.setIsActive(cardType.getIsActive());
        }

        cardTypePolicy.initialize(existingCardType);

        if (cardTypePort.existsByCodeAndCardTypeIdNot(existingCardType.getCode(), cardTypeId)) {
            throw new ConflictException("Card type code already exists");
        }

        return cardTypePort.save(existingCardType);
    }

    @Override
    @Transactional(readOnly = true)
    public CardType getCardTypeById(UUID cardTypeId) {
        currentAccountPortIn.requirePermission(CARD_TYPE_READ_ALL);
        return findExistingCardType(cardTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardType> getCardTypes(Boolean isActive) {
        requireCatalogReadForOperation();
        return cardTypePort.findAll(isActive);
    }

    @Override
    @Transactional
    public void deleteCardType(UUID cardTypeId) {
        currentAccountPortIn.requirePermission(CARD_TYPE_DELETE_ALL);
        CardType existingCardType = findExistingCardType(cardTypeId);
        if (Boolean.FALSE.equals(existingCardType.getIsActive())) {
            return;
        }
        if (cardTypePort.hasActiveCards(cardTypeId)) {
            throw new ConflictException("Card type is used by active cards");
        }

        cardTypePolicy.deactivate(existingCardType);
        cardTypePort.save(existingCardType);
    }

    @Override
    @Transactional
    public CardType activateCardType(UUID cardTypeId) {
        currentAccountPortIn.requirePermission(CARD_TYPE_UPDATE_ALL);
        CardType existingCardType = findExistingCardType(cardTypeId);
        if (Boolean.TRUE.equals(existingCardType.getIsActive())) {
            return existingCardType;
        }

        cardTypePolicy.activate(existingCardType);
        if (cardTypePort.existsByCodeAndCardTypeIdNot(existingCardType.getCode(), cardTypeId)) {
            throw new ConflictException("Card type code already exists");
        }

        return cardTypePort.save(existingCardType);
    }

    private CardType findExistingCardType(UUID cardTypeId) {
        return cardTypePort.findById(cardTypeId)
                .orElseThrow(() -> new NotFoundException("Card type not found"));
    }

    private void requireCatalogReadForOperation() {
        if (currentAccountPortIn.hasPermission(CARD_TYPE_READ_ALL)
                || currentAccountPortIn.hasPermission(PARKING_SESSION_CHECK_IN_ALL)
                || currentAccountPortIn.hasPermission(PARKING_SESSION_CHECK_OUT_ALL)) {
            return;
        }

        currentAccountPortIn.requirePermission(CARD_TYPE_READ_ALL);
    }
}

