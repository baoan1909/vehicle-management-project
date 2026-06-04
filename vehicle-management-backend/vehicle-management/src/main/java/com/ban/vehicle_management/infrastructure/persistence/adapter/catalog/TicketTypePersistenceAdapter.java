package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.infrastructure.mapper.catalog.TicketTypePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.TicketTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.TicketTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog.TicketTypeSpecifications;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TicketTypePersistenceAdapter implements TicketTypePortOut {
    private final TicketTypeRepository ticketTypeRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TicketTypePersistenceMapper ticketTypePersistenceMapper;

    public  TicketTypePersistenceAdapter(
            TicketTypeRepository ticketTypeRepository,
            PriceRuleRepository priceRuleRepository,
            SubscriptionRepository subscriptionRepository,
            TicketTypePersistenceMapper ticketTypePersistenceAdapter
    ){
        this.ticketTypeRepository = ticketTypeRepository;
        this.priceRuleRepository = priceRuleRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.ticketTypePersistenceMapper = ticketTypePersistenceAdapter;
    }

    @Override
    public TicketType save(TicketType ticketType){
        TicketTypeEntity savedTicketType = ticketTypeRepository.saveAndFlush(ticketTypePersistenceMapper.toEntity(ticketType));
        return  ticketTypePersistenceMapper.toDomain(savedTicketType);
    }

    @Override
    public Optional<TicketType> findById(UUID ticketTypeId){
        return ticketTypeRepository.findById(ticketTypeId).map(ticketTypePersistenceMapper::toDomain);
    }

    @Override
    public List<TicketType> findAll(TicketTypeStatus status, String keyword) {
        return ticketTypeRepository.findAll(TicketTypeSpecifications.withFilters(status, keyword))
                .stream()
                .map(ticketTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveByCode(String code){
        return ticketTypeRepository.existsByCodeAndStatus(code, TicketTypeStatus.ACTIVE);
    }

    @Override
    public  boolean existsActiveByCodeAndTicketTypeIdNot(String code, UUID ticketTypeId){
        return ticketTypeRepository.existsByCodeAndStatusAndTicketTypeIdNot(
                code,
                TicketTypeStatus.ACTIVE,
                ticketTypeId
        );
    }

    @Override
    public boolean hasActivePriceRules(UUID ticketTypeId){
        return priceRuleRepository.existsByTicketTypeIdAndIsActiveTrue(ticketTypeId);
    }

    @Override
    public  boolean hasBlockingSubcriptions(UUID ticketTypeId){
        return subscriptionRepository.existsByTicketTypeIdAndStatusIn(
                ticketTypeId,
                List.of(SubscriptionStatus.PENDING, SubscriptionStatus.ACTIVE)
        );
    }

}
