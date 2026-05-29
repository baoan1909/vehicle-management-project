package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.infrastructure.mapper.catalog.PriceRulePersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.catalog.TicketTypePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.TicketTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.TicketTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PriceRulePersistenceAdapterTest {

    @Mock
    private PriceRuleRepository priceRuleRepository;

    @Mock
    private VehicleTypeRepository vehicleTypeRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PriceRulePersistenceMapper priceRulePersistenceMapper;

    @Mock
    private TicketTypePersistenceMapper ticketTypePersistenceMapper;

    @InjectMocks
    private PriceRulePersistenceAdapter priceRulePersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingPriceRule() {
        PriceRule priceRule = new PriceRule();
        priceRule.setPriceRuleId(UUID.randomUUID());

        PriceRuleEntity priceRuleEntity = new PriceRuleEntity();

        when(priceRulePersistenceMapper.toEntity(priceRule)).thenReturn(priceRuleEntity);
        when(priceRuleRepository.saveAndFlush(priceRuleEntity)).thenReturn(priceRuleEntity);
        when(priceRulePersistenceMapper.toDomain(priceRuleEntity)).thenReturn(priceRule);

        PriceRule savedPriceRule = priceRulePersistenceAdapter.save(priceRule);

        assertEquals(priceRule, savedPriceRule);
        verify(priceRuleRepository).saveAndFlush(priceRuleEntity);
    }

    @Test
    void shouldMapEntityWhenFindingById() {
        UUID priceRuleId = UUID.randomUUID();
        PriceRuleEntity priceRuleEntity = new PriceRuleEntity();
        PriceRule priceRule = new PriceRule();
        priceRule.setPriceRuleId(priceRuleId);

        when(priceRuleRepository.findById(priceRuleId)).thenReturn(Optional.of(priceRuleEntity));
        when(priceRulePersistenceMapper.toDomain(priceRuleEntity)).thenReturn(priceRule);

        Optional<PriceRule> result = priceRulePersistenceAdapter.findById(priceRuleId);

        assertTrue(result.isPresent());
        assertEquals(priceRuleId, result.get().getPriceRuleId());
    }

    @Test
    void shouldReturnMappedListWhenFindingAll() {
        PriceRuleEntity firstEntity = new PriceRuleEntity();
        PriceRuleEntity secondEntity = new PriceRuleEntity();
        PriceRule firstPriceRule = new PriceRule();
        PriceRule secondPriceRule = new PriceRule();

        when(priceRuleRepository.findAll(any(Specification.class))).thenReturn(List.of(firstEntity, secondEntity));
        when(priceRulePersistenceMapper.toDomain(firstEntity)).thenReturn(firstPriceRule);
        when(priceRulePersistenceMapper.toDomain(secondEntity)).thenReturn(secondPriceRule);

        List<PriceRule> result = priceRulePersistenceAdapter.findAll(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Boolean.TRUE,
                "ban ngay"
        );

        assertEquals(2, result.size());
        assertEquals(firstPriceRule, result.get(0));
        assertEquals(secondPriceRule, result.get(1));
    }

    @Test
    void shouldDelegateActiveVehicleTypeCheck() {
        UUID vehicleTypeId = UUID.randomUUID();

        when(vehicleTypeRepository.existsByVehicleTypeIdAndIsActiveTrue(vehicleTypeId)).thenReturn(true);

        boolean exists = priceRulePersistenceAdapter.existsActiveVehicleTypeById(vehicleTypeId);

        assertTrue(exists);
        verify(vehicleTypeRepository).existsByVehicleTypeIdAndIsActiveTrue(vehicleTypeId);
    }

    @Test
    void shouldReturnEmptyWhenFindingActiveTicketTypeWithNullId() {
        Optional<TicketType> result = priceRulePersistenceAdapter.findActiveTicketTypeById(null);

        assertTrue(result.isEmpty());
        verify(ticketTypeRepository, never()).findById(any());
    }

    @Test
    void shouldMapTicketTypeWhenTicketTypeIsActive() {
        UUID ticketTypeId = UUID.randomUUID();
        TicketTypeEntity ticketTypeEntity = new TicketTypeEntity();
        ticketTypeEntity.setTicketTypeId(ticketTypeId);
        ticketTypeEntity.setIsActive(Boolean.TRUE);

        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(ticketTypeId);

        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(ticketTypeEntity));
        when(ticketTypePersistenceMapper.toDomain(ticketTypeEntity)).thenReturn(ticketType);

        Optional<TicketType> result = priceRulePersistenceAdapter.findActiveTicketTypeById(ticketTypeId);

        assertTrue(result.isPresent());
        assertEquals(ticketTypeId, result.get().getTicketTypeId());
    }

    @Test
    void shouldReturnEmptyWhenTicketTypeIsInactive() {
        UUID ticketTypeId = UUID.randomUUID();
        TicketTypeEntity ticketTypeEntity = new TicketTypeEntity();
        ticketTypeEntity.setIsActive(Boolean.FALSE);

        when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(ticketTypeEntity));

        Optional<TicketType> result = priceRulePersistenceAdapter.findActiveTicketTypeById(ticketTypeId);

        assertTrue(result.isEmpty());
        verify(ticketTypePersistenceMapper, never()).toDomain(any());
    }

    @Test
    void shouldDelegateVisitorTimeOverlapCheck() {
        UUID pricePlanId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        UUID excludedPriceRuleId = UUID.randomUUID();
        LocalTime timeFrom = LocalTime.of(6, 0, 0);
        LocalTime timeTo = LocalTime.of(17, 59, 59);

        when(priceRuleRepository.existsActiveVisitorTimeOverlap(
                pricePlanId,
                vehicleTypeId,
                ticketTypeId,
                timeFrom,
                timeTo,
                excludedPriceRuleId
        )).thenReturn(true);

        boolean exists = priceRulePersistenceAdapter.existsActiveVisitorTimeOverlap(
                pricePlanId,
                vehicleTypeId,
                ticketTypeId,
                timeFrom,
                timeTo,
                excludedPriceRuleId
        );

        assertTrue(exists);
        verify(priceRuleRepository).existsActiveVisitorTimeOverlap(
                pricePlanId,
                vehicleTypeId,
                ticketTypeId,
                timeFrom,
                timeTo,
                excludedPriceRuleId
        );
    }

    @Test
    void shouldDelegateCustomerRuleCheck() {
        UUID pricePlanId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        UUID excludedPriceRuleId = UUID.randomUUID();

        when(priceRuleRepository.existsActiveCustomerRule(
                pricePlanId,
                vehicleTypeId,
                ticketTypeId,
                excludedPriceRuleId
        )).thenReturn(true);

        boolean exists = priceRulePersistenceAdapter.existsActiveCustomerRule(
                pricePlanId,
                vehicleTypeId,
                ticketTypeId,
                excludedPriceRuleId
        );

        assertTrue(exists);
        verify(priceRuleRepository).existsActiveCustomerRule(
                pricePlanId,
                vehicleTypeId,
                ticketTypeId,
                excludedPriceRuleId
        );
    }

    @Test
    void shouldReturnTrueWhenSubscriptionUsesPriceRule() {
        UUID priceRuleId = UUID.randomUUID();

        when(subscriptionRepository.existsByPriceRuleId(priceRuleId)).thenReturn(true);

        boolean hasUsage = priceRulePersistenceAdapter.hasUsage(priceRuleId);

        assertTrue(hasUsage);
        verify(subscriptionRepository).existsByPriceRuleId(priceRuleId);
    }

    @Test
    void shouldReturnFalseWhenPriceRuleHasNoUsage() {
        UUID priceRuleId = UUID.randomUUID();

        when(subscriptionRepository.existsByPriceRuleId(priceRuleId)).thenReturn(false);

        boolean hasUsage = priceRulePersistenceAdapter.hasUsage(priceRuleId);

        assertEquals(false, hasUsage);
        verify(subscriptionRepository).existsByPriceRuleId(priceRuleId);
    }
}
