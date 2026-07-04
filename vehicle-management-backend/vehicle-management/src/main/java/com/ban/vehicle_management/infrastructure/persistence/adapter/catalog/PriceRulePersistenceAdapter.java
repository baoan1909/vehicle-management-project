package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.infrastructure.mapper.catalog.PriceRulePersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.catalog.TicketTypePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.TicketTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog.PriceRuleSpecifications;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import org.springframework.stereotype.Component;

@Component
public class PriceRulePersistenceAdapter implements PriceRulePortOut {

    private final PriceRuleRepository priceRuleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PriceRulePersistenceMapper priceRulePersistenceMapper;
    private final TicketTypePersistenceMapper ticketTypePersistenceMapper;

    public PriceRulePersistenceAdapter(
            PriceRuleRepository priceRuleRepository,
            VehicleTypeRepository vehicleTypeRepository,
            TicketTypeRepository ticketTypeRepository,
            SubscriptionRepository subscriptionRepository,
            PriceRulePersistenceMapper priceRulePersistenceMapper,
            TicketTypePersistenceMapper ticketTypePersistenceMapper
    ) {
        this.priceRuleRepository = priceRuleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.priceRulePersistenceMapper = priceRulePersistenceMapper;
        this.ticketTypePersistenceMapper = ticketTypePersistenceMapper;
    }

    @Override
    public PriceRule save(PriceRule priceRule) {
        PriceRuleEntity savedEntity = priceRuleRepository.saveAndFlush(
                priceRulePersistenceMapper.toEntity(priceRule)
        );
        return priceRulePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PriceRule> findById(UUID priceRuleId) {
        return priceRuleRepository.findById(priceRuleId)
                .map(priceRulePersistenceMapper::toDomain);
    }

    @Override
    public List<PriceRule> findAll(
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            Boolean isActive,
            String keyword
    ) {
        return priceRuleRepository.findAll(
                        PriceRuleSpecifications.withFilters(pricePlanId, vehicleTypeId, ticketTypeId, isActive, keyword)
                )
                .stream()
                .map(priceRulePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveVehicleTypeById(UUID vehicleTypeId) {
        return vehicleTypeRepository.existsByVehicleTypeIdAndIsActiveTrue(vehicleTypeId);
    }

    @Override
    public Optional<TicketType> findActiveTicketTypeById(UUID ticketTypeId) {
        if (ticketTypeId == null) {
            return Optional.empty();
        }

        return ticketTypeRepository.findById(ticketTypeId)
                .filter(ticketType -> ticketType.getStatus() == TicketTypeStatus.ACTIVE)
                .map(ticketTypePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsActiveVisitorTimeOverlap(
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            LocalTime timeFrom,
            LocalTime timeTo,
            UUID excludedPriceRuleId
    ) {
        return priceRuleRepository.findActiveVisitorRulesForOverlap(
                        pricePlanId,
                        vehicleTypeId,
                        ticketTypeId,
                        excludedPriceRuleId
                )
                .stream()
                .anyMatch(existingRule -> overlaps(
                        existingRule.getTimeFrom(),
                        existingRule.getTimeTo(),
                        timeFrom,
                        timeTo
                ));
    }

    private boolean overlaps(
            LocalTime existingFrom,
            LocalTime existingTo,
            LocalTime newFrom,
            LocalTime newTo
    ) {
        List<TimeRange> existingRanges = splitRange(existingFrom, existingTo);
        List<TimeRange> newRanges = splitRange(newFrom, newTo);

        return existingRanges.stream()
                .anyMatch(existingRange -> newRanges.stream()
                        .anyMatch(newRange -> rangesOverlap(existingRange, newRange)));
    }

    private List<TimeRange> splitRange(LocalTime from, LocalTime to) {
        int start = from.toSecondOfDay();
        int end = to.toSecondOfDay();

        if (start < end) {
            return List.of(new TimeRange(start, end));
        }

        return List.of(
                new TimeRange(start, 86399),
                new TimeRange(0, end)
        );
    }

    private boolean rangesOverlap(TimeRange first, TimeRange second) {
        return first.start <= second.end && second.start <= first.end;
    }

    private record TimeRange(int start, int end) {
    }

    @Override
    public boolean existsActiveCustomerRule(
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            UUID excludedPriceRuleId
    ) {
        return priceRuleRepository.existsActiveCustomerRule(
                pricePlanId,
                vehicleTypeId,
                ticketTypeId,
                excludedPriceRuleId
        );
    }

    @Override
    public boolean hasUsage(UUID priceRuleId) {
        return subscriptionRepository.existsByPriceRuleId(priceRuleId);
    }

    @Override
    public Optional<PriceRule> findActiveSubscriptionRule(
            UUID vehicleTypeId,
            UUID ticketTypeId,
            LocalDate effectiveDate
    ) {
        return priceRuleRepository.findActiveSubscriptionRule(vehicleTypeId, ticketTypeId, effectiveDate)
                .map(priceRulePersistenceMapper::toDomain);
    }

    @Override
    public Optional<PriceRule> findActiveVisitorRuleByTime(
            UUID vehicleTypeId,
            LocalDate effectiveDate,
            LocalTime localTime
    ) {
        return priceRuleRepository.findActiveVisitorRuleByTime(vehicleTypeId, effectiveDate, localTime)
                .map(priceRulePersistenceMapper::toDomain);
    }
}
