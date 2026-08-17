package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.infrastructure.mapper.catalog.CardTypePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.CardTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.CardRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.CardTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog.CardTypeSpecifications;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class CardTypePersistenceAdapter implements CardTypePortOut {

    private final CardTypeRepository cardTypeRepository;
    private final CardRepository cardRepository;
    private final CardTypePersistenceMapper cardTypePersistenceMapper;

    public CardTypePersistenceAdapter(
            CardTypeRepository cardTypeRepository,
            CardRepository cardRepository,
            CardTypePersistenceMapper cardTypePersistenceMapper
    ) {
        this.cardTypeRepository = cardTypeRepository;
        this.cardRepository = cardRepository;
        this.cardTypePersistenceMapper = cardTypePersistenceMapper;
    }

    @Override
    public CardType save(CardType cardType) {
        CardTypeEntity cardTypeEntity = cardTypePersistenceMapper.toEntity(cardType);
        CardTypeEntity savedCardTypeEntity = cardTypeRepository.saveAndFlush(cardTypeEntity);
        return cardTypePersistenceMapper.toDomain(savedCardTypeEntity);
    }

    @Override
    public Optional<CardType> findById(UUID cardTypeId) {
        return cardTypeRepository.findById(cardTypeId)
                .map(cardTypePersistenceMapper::toDomain);
    }

    @Override
    public List<CardType> findAll(Boolean isActive) {
        Specification<CardTypeEntity> specification = CardTypeSpecifications.withFilters(isActive);
        return cardTypeRepository.findAll(specification).stream()
                .map(cardTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return cardTypeRepository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeAndCardTypeIdNot(String code, UUID cardTypeId) {
        return cardTypeRepository.existsByCodeAndCardTypeIdNot(code, cardTypeId);
    }

    @Override
    public boolean hasActiveCards(UUID cardTypeId) {
        return cardRepository.existsByCardTypeIdAndStatusIn(
                cardTypeId,
                List.of(
                        CardStatus.AVAILABLE,
                        CardStatus.ASSIGNED,
                        CardStatus.IN_USE,
                        CardStatus.BLOCKED,
                        CardStatus.LOST
                )
        );
    }
}



