package com.ban.vehicle_management.application.catalog.cardtype.usecase;

import com.ban.vehicle_management.application.catalog.cardtype.port.in.CardTypeUseCase;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePort;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.domain.catalog.cardtype.policy.CardTypePolicy;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardTypeUseCaseImpl implements CardTypeUseCase {

    private final CardTypePort cardTypePort;
    private final CardTypePolicy cardTypePolicy = new CardTypePolicy();

    public CardTypeUseCaseImpl(CardTypePort cardTypePort) {
        this.cardTypePort = cardTypePort;
    }

    @Override
    @Transactional
    public CardType createCardType(CardType cardType) {
        cardTypePolicy.initialize(cardType);

        if (cardTypePort.existsByCode(cardType.getCode())) {
            throw new BadRequestException("Card type code already exists");
        }

        cardType.setCardTypeId(UUID.randomUUID());
        return cardTypePort.save(cardType);
    }

    @Override
    @Transactional
    public CardType updateCardType(UUID cardTypeId, CardType cardType) {
        CardType existingCardType = getCardTypeById(cardTypeId);

        existingCardType.setCode(cardType.getCode());
        existingCardType.setName(cardType.getName());
        existingCardType.setDescription(cardType.getDescription());
        if (cardType.getIsReturnRequired() != null) {
            existingCardType.setIsReturnRequired(cardType.getIsReturnRequired());
        }

        cardTypePolicy.initialize(existingCardType);

        if (cardTypePort.existsByCodeAndCardTypeIdNot(existingCardType.getCode(), cardTypeId)) {
            throw new BadRequestException("Card type code already exists");
        }

        return cardTypePort.save(existingCardType);
    }

    @Override
    @Transactional(readOnly = true)
    public CardType getCardTypeById(UUID cardTypeId) {
        return cardTypePort.findById(cardTypeId)
                .orElseThrow(() -> new NotFoundException("Card type not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardType> getCardTypes() {
        return cardTypePort.findAll();
    }

    @Override
    @Transactional
    public void deleteCardType(UUID cardTypeId) {
        getCardTypeById(cardTypeId);

        if (cardTypePort.existsInUse(cardTypeId)) {
            throw new BadRequestException("Card type is in use and cannot be deleted");
        }

        cardTypePort.deleteById(cardTypeId);
    }
}

