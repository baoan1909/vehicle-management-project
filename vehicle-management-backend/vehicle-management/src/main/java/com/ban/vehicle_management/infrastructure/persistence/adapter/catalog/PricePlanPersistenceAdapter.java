package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.priceplan.port.out.PricePlanPortOut;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.infrastructure.mapper.catalog.PricePlanPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PricePlanEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PricePlanRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog.PricePlanSpecifications;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PricePlanPersistenceAdapter implements PricePlanPortOut {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final PricePlanRepository pricePlanRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final PricePlanPersistenceMapper pricePlanPersistenceMapper;

    public PricePlanPersistenceAdapter(
            PricePlanRepository pricePlanRepository,
            PriceRuleRepository priceRuleRepository,
            PricePlanPersistenceMapper pricePlanPersistenceMapper
    ) {
        this.pricePlanRepository = pricePlanRepository;
        this.priceRuleRepository = priceRuleRepository;
        this.pricePlanPersistenceMapper = pricePlanPersistenceMapper;
    }

    @Override
    public PricePlan save(PricePlan pricePlan) {
        PricePlanEntity savedPricePlanEntity = pricePlanRepository.saveAndFlush(
                pricePlanPersistenceMapper.toEntity(pricePlan)
        );
        return pricePlanPersistenceMapper.toDomain(savedPricePlanEntity);
    }

    @Override
    public Optional<PricePlan> findById(UUID pricePlanId) {
        return pricePlanRepository.findById(pricePlanId)
                .map(pricePlanPersistenceMapper::toDomain);
    }

    @Override
    public List<PricePlan> findAll(
            Boolean isActive,
            PricePlanAppliesTo appliesTo,
            LocalDate effectiveDate,
            String keyword
    ) {
        return pricePlanRepository.findAll(
                        PricePlanSpecifications.withFilters(isActive, appliesTo, effectiveDate, keyword)
                )
                .stream()
                .map(pricePlanPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return pricePlanRepository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeAndPricePlanIdNot(String code, UUID pricePlanId) {
        return pricePlanRepository.existsByCodeAndPricePlanIdNot(code, pricePlanId);
    }

    @Override
    public boolean existsActiveOverlap(
            PricePlanAppliesTo appliesTo,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            UUID excludedPricePlanId
    ) {
        LocalDate effectiveToBoundary = effectiveTo == null ? MAX_DATE : effectiveTo;
        return pricePlanRepository.existsActiveOverlap(
                appliesTo,
                effectiveFrom,
                effectiveToBoundary,
                excludedPricePlanId
        );
    }

    @Override
    public boolean hasRules(UUID pricePlanId) {
        return priceRuleRepository.existsByPricePlanId(pricePlanId);
    }
}