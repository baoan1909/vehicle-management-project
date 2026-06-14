package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.infrastructure.mapper.catalog.CardTypePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.CardTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.CardRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.CardTypeRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardTypePersistenceAdapterTest {

    @Mock
    private CardTypeRepository cardTypeRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardTypePersistenceMapper cardTypePersistenceMapper;

    @InjectMocks
    private CardTypePersistenceAdapter cardTypePersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingCardType() {
        CardType cardType = new CardType();
        cardType.setCardTypeId(UUID.randomUUID());

        CardTypeEntity cardTypeEntity = new CardTypeEntity();

        when(cardTypePersistenceMapper.toEntity(cardType)).thenReturn(cardTypeEntity);
        when(cardTypeRepository.saveAndFlush(cardTypeEntity)).thenReturn(cardTypeEntity);
        when(cardTypePersistenceMapper.toDomain(cardTypeEntity)).thenReturn(cardType);

        cardTypePersistenceAdapter.save(cardType);

        verify(cardTypeRepository).saveAndFlush(cardTypeEntity);
    }
}
