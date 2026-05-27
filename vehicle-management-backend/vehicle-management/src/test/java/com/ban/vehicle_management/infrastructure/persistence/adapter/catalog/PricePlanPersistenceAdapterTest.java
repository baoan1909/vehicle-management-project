package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.infrastructure.mapper.catalog.PricePlanPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PricePlanEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PricePlanRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import java.time.LocalDate;
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
class PricePlanPersistenceAdapterTest {

    @Mock
    private PricePlanRepository pricePlanRepository;

    @Mock
    private PriceRuleRepository priceRuleRepository;

    @Mock
    private PricePlanPersistenceMapper pricePlanPersistenceMapper;

    @InjectMocks
    private PricePlanPersistenceAdapter pricePlanPersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingPricePlan() {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setPricePlanId(UUID.randomUUID());

        PricePlanEntity pricePlanEntity = new PricePlanEntity();

        when(pricePlanPersistenceMapper.toEntity(pricePlan)).thenReturn(pricePlanEntity);
        when(pricePlanRepository.saveAndFlush(pricePlanEntity)).thenReturn(pricePlanEntity);
        when(pricePlanPersistenceMapper.toDomain(pricePlanEntity)).thenReturn(pricePlan);

        PricePlan savedPricePlan = pricePlanPersistenceAdapter.save(pricePlan);

        assertEquals(pricePlan, savedPricePlan);
        verify(pricePlanRepository).saveAndFlush(pricePlanEntity);
    }

    @Test
    void shouldMapEntityWhenFindingById() {
        UUID pricePlanId = UUID.randomUUID();
        PricePlanEntity pricePlanEntity = new PricePlanEntity();
        PricePlan pricePlan = new PricePlan();
        pricePlan.setPricePlanId(pricePlanId);

        when(pricePlanRepository.findById(pricePlanId)).thenReturn(Optional.of(pricePlanEntity));
        when(pricePlanPersistenceMapper.toDomain(pricePlanEntity)).thenReturn(pricePlan);

        Optional<PricePlan> result = pricePlanPersistenceAdapter.findById(pricePlanId);

        assertTrue(result.isPresent());
        assertEquals(pricePlanId, result.get().getPricePlanId());
    }

    @Test
    void shouldReturnMappedListWhenFindingAll() {
        PricePlanEntity firstEntity = new PricePlanEntity();
        PricePlanEntity secondEntity = new PricePlanEntity();
        PricePlan firstPricePlan = new PricePlan();
        PricePlan secondPricePlan = new PricePlan();

        when(pricePlanRepository.findAll(any(Specification.class))).thenReturn(List.of(firstEntity, secondEntity));
        when(pricePlanPersistenceMapper.toDomain(firstEntity)).thenReturn(firstPricePlan);
        when(pricePlanPersistenceMapper.toDomain(secondEntity)).thenReturn(secondPricePlan);

        List<PricePlan> result = pricePlanPersistenceAdapter.findAll(
                Boolean.TRUE,
                PricePlanAppliesTo.VISITOR,
                LocalDate.of(2027, 1, 1),
                "VISITOR"
        );

        assertEquals(2, result.size());
        assertEquals(firstPricePlan, result.get(0));
        assertEquals(secondPricePlan, result.get(1));
    }

    @Test
    void shouldDelegateExistsByCode() {
        when(pricePlanRepository.existsByCode("VISITOR-2027")).thenReturn(true);

        boolean exists = pricePlanPersistenceAdapter.existsByCode("VISITOR-2027");

        assertTrue(exists);
        verify(pricePlanRepository).existsByCode("VISITOR-2027");
    }

    @Test
    void shouldDelegateExistsByCodeAndPricePlanIdNot() {
        UUID pricePlanId = UUID.randomUUID();

        when(pricePlanRepository.existsByCodeAndPricePlanIdNot("VISITOR-2027", pricePlanId)).thenReturn(true);

        boolean exists = pricePlanPersistenceAdapter.existsByCodeAndPricePlanIdNot("VISITOR-2027", pricePlanId);

        assertTrue(exists);
        verify(pricePlanRepository).existsByCodeAndPricePlanIdNot("VISITOR-2027", pricePlanId);
    }

    @Test
    void shouldUseMaxDateWhenCheckingOverlapWithOpenEndedEffectiveTo() {
        UUID excludedPricePlanId = UUID.randomUUID();
        LocalDate effectiveFrom = LocalDate.of(2027, 1, 1);

        when(pricePlanRepository.existsActiveOverlap(
                PricePlanAppliesTo.VISITOR,
                effectiveFrom,
                LocalDate.of(9999, 12, 31),
                excludedPricePlanId
        )).thenReturn(true);

        boolean exists = pricePlanPersistenceAdapter.existsActiveOverlap(
                PricePlanAppliesTo.VISITOR,
                effectiveFrom,
                null,
                excludedPricePlanId
        );

        assertTrue(exists);
        verify(pricePlanRepository).existsActiveOverlap(
                PricePlanAppliesTo.VISITOR,
                effectiveFrom,
                LocalDate.of(9999, 12, 31),
                excludedPricePlanId
        );
    }

    @Test
    void shouldDelegateHasRulesToPriceRuleRepository() {
        UUID pricePlanId = UUID.randomUUID();

        when(priceRuleRepository.existsByPricePlanId(pricePlanId)).thenReturn(true);

        boolean hasRules = pricePlanPersistenceAdapter.hasRules(pricePlanId);

        assertTrue(hasRules);
        verify(priceRuleRepository).existsByPricePlanId(pricePlanId);
    }
}
