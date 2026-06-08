package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketCategoryPortOut;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.infrastructure.mapper.operations.SupportTicketCategoryPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketCategoryEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketCategoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.operations.SupportTicketCategorySpecifications;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketCategoryPersistenceAdapter implements SupportTicketCategoryPortOut {

    private final SupportTicketCategoryRepository categoryRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketCategoryPersistenceMapper categoryPersistenceMapper;

    public SupportTicketCategoryPersistenceAdapter(
            SupportTicketCategoryRepository categoryRepository,
            SupportTicketRepository supportTicketRepository,
            SupportTicketCategoryPersistenceMapper categoryPersistenceMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.categoryPersistenceMapper = categoryPersistenceMapper;
    }

    @Override
    public SupportTicketCategory save(SupportTicketCategory category) {
        SupportTicketCategoryEntity savedEntity = categoryRepository.saveAndFlush(categoryPersistenceMapper.toEntity(category));
        return categoryPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<SupportTicketCategory> findById(UUID categoryId) {
        return categoryRepository.findById(categoryId).map(categoryPersistenceMapper::toDomain);
    }

    @Override
    public List<SupportTicketCategory> findAll(
            SupportTicketCategoryStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    ) {
        return categoryRepository.findAll(SupportTicketCategorySpecifications.withFilters(status, priority, keyword))
                .stream()
                .map(categoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveByCode(String code) {
        return categoryRepository.existsByCodeAndStatus(code, SupportTicketCategoryStatus.ACTIVE);
    }

    @Override
    public boolean existsActiveByCodeAndCategoryIdNot(String code, UUID categoryId) {
        return categoryRepository.existsByCodeAndStatusAndCategoryIdNot(
                code,
                SupportTicketCategoryStatus.ACTIVE,
                categoryId
        );
    }

    @Override
    public boolean hasUnfinishedTickets(UUID categoryId) {
        return supportTicketRepository.existsByCategoryIdAndStatusIn(
                categoryId,
                List.of(
                        SupportTicketStatus.OPEN,
                        SupportTicketStatus.IN_PROGRESS,
                        SupportTicketStatus.RESOLVED
                )
        );
    }
}