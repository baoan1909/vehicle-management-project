package com.ban.vehicle_management.infrastructure.persistence.adapter.accesscontrol;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.infrastructure.mapper.accesscontrol.CardPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.CardRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardPersistenceAdapterTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private LostCardReportRepository lostCardReportRepository;

    @Mock
    private ParkingSessionRepository parkingSessionRepository;

    @Mock
    private CardPersistenceMapper cardPersistenceMapper;

    @InjectMocks
    private CardPersistenceAdapter cardPersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingCard() {
        Card card = new Card();
        card.setCardId(UUID.randomUUID());

        CardEntity cardEntity = new CardEntity();

        when(cardPersistenceMapper.toEntity(card)).thenReturn(cardEntity);
        when(cardRepository.saveAndFlush(cardEntity)).thenReturn(cardEntity);
        when(cardPersistenceMapper.toDomain(cardEntity)).thenReturn(card);

        cardPersistenceAdapter.save(card);

        verify(cardRepository).saveAndFlush(cardEntity);
    }
}
