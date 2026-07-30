package com.ban.vehicle_management.infrastructure.persistence.adapter.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.infrastructure.mapper.accesscontrol.SubscriptionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SubscriptionPersistenceAdapterTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionPersistenceMapper subscriptionPersistenceMapper;

    @InjectMocks
    private SubscriptionPersistenceAdapter subscriptionPersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingSubscription() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        SubscriptionEntity entity = new SubscriptionEntity();

        when(subscriptionPersistenceMapper.toEntity(subscription)).thenReturn(entity);
        when(subscriptionRepository.saveAndFlush(entity)).thenReturn(entity);
        when(subscriptionPersistenceMapper.toDomain(entity)).thenReturn(subscription);

        Subscription savedSubscription = subscriptionPersistenceAdapter.save(subscription);

        assertEquals(subscription, savedSubscription);
        verify(subscriptionRepository).saveAndFlush(entity);
    }

    @Test
    void shouldMapEntityWhenFindingById() {
        UUID subscriptionId = UUID.randomUUID();
        SubscriptionEntity entity = new SubscriptionEntity();
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(subscriptionId);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(entity));
        when(subscriptionPersistenceMapper.toDomain(entity)).thenReturn(subscription);

        Optional<Subscription> result = subscriptionPersistenceAdapter.findById(subscriptionId);

        assertTrue(result.isPresent());
        assertEquals(subscriptionId, result.get().getSubscriptionId());
    }

    @Test
    void shouldReturnEmptyWhenFindingByIdAndEntityDoesNotExist() {
        UUID subscriptionId = UUID.randomUUID();
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        Optional<Subscription> result = subscriptionPersistenceAdapter.findById(subscriptionId);

        assertTrue(result.isEmpty());
        verify(subscriptionPersistenceMapper, never()).toDomain(any(SubscriptionEntity.class));
    }

    @Test
    void shouldReturnMappedListWhenFindingAll() {
        SubscriptionEntity firstEntity = new SubscriptionEntity();
        SubscriptionEntity secondEntity = new SubscriptionEntity();
        Subscription firstSubscription = new Subscription();
        Subscription secondSubscription = new Subscription();

        when(subscriptionRepository.findAll(any(Specification.class))).thenReturn(List.of(firstEntity, secondEntity));
        when(subscriptionPersistenceMapper.toDomain(firstEntity)).thenReturn(firstSubscription);
        when(subscriptionPersistenceMapper.toDomain(secondEntity)).thenReturn(secondSubscription);

        List<Subscription> result = subscriptionPersistenceAdapter.findAll(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                SubscriptionStatus.ACTIVE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "59A1"
        );

        assertEquals(2, result.size());
        assertEquals(firstSubscription, result.get(0));
        assertEquals(secondSubscription, result.get(1));
    }

    @Test
    void shouldCheckOverlapWithBlockingStatuses() {
        UUID customerVehicleId = UUID.randomUUID();
        UUID excludedSubscriptionId = UUID.randomUUID();
        LocalDate effectiveFrom = LocalDate.of(2026, 6, 20);
        LocalDate effectiveTo = LocalDate.of(2026, 7, 19);

        when(subscriptionRepository.existsOverlappingSubscription(
                eq(customerVehicleId),
                eq(effectiveFrom),
                eq(effectiveTo),
                any(Collection.class),
                eq(excludedSubscriptionId)
        )).thenReturn(true);

        boolean exists = subscriptionPersistenceAdapter.existsOverlappingSubscription(
                customerVehicleId,
                effectiveFrom,
                effectiveTo,
                excludedSubscriptionId
        );

        assertTrue(exists);

        ArgumentCaptor<Collection<SubscriptionStatus>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(subscriptionRepository).existsOverlappingSubscription(
                eq(customerVehicleId),
                eq(effectiveFrom),
                eq(effectiveTo),
                statusesCaptor.capture(),
                eq(excludedSubscriptionId)
        );
        assertEquals(
                List.of(
                        SubscriptionStatus.PENDING,
                        SubscriptionStatus.PENDING_PAYMENT,
                        SubscriptionStatus.PENDING_CARD,
                        SubscriptionStatus.ACTIVE
                ),
                List.copyOf(statusesCaptor.getValue())
        );
    }

    @Test
    void shouldCountCapacityHoldingSubscriptionsByVehicleTypeId() {
        UUID vehicleTypeId = UUID.randomUUID();

        when(subscriptionRepository.countByVehicleTypeIdAndStatusIn(
                eq(vehicleTypeId),
                any(Collection.class)
        )).thenReturn(3L);

        long count = subscriptionPersistenceAdapter.countReservedOrActiveByVehicleTypeId(vehicleTypeId);

        assertEquals(3L, count);

        ArgumentCaptor<Collection<SubscriptionStatus>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(subscriptionRepository).countByVehicleTypeIdAndStatusIn(
                eq(vehicleTypeId),
                statusesCaptor.capture()
        );
        assertEquals(
                List.of(
                        SubscriptionStatus.PENDING_PAYMENT,
                        SubscriptionStatus.PENDING_CARD,
                        SubscriptionStatus.ACTIVE
                ),
                List.copyOf(statusesCaptor.getValue())
        );
    }

    @Test
    void shouldDelegateExpireActiveSubscriptionsBeforeWithStatusBoundary() {
        LocalDate businessDate = LocalDate.of(2026, 7, 30);

        when(subscriptionRepository.expireActiveSubscriptionsBefore(
                businessDate,
                SubscriptionStatus.ACTIVE,
                SubscriptionStatus.EXPIRED
        )).thenReturn(5);

        int expiredCount = subscriptionPersistenceAdapter.expireActiveSubscriptionsBefore(businessDate);

        assertEquals(5, expiredCount);
        verify(subscriptionRepository).expireActiveSubscriptionsBefore(
                businessDate,
                SubscriptionStatus.ACTIVE,
                SubscriptionStatus.EXPIRED
        );
    }
}
