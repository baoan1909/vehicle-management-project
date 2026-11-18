package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.supportticketcategory.port.out.SupportTicketCategoryPortOut;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.infrastructure.mapper.operations.SupportTicketCategoryPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketCategoryEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketCategoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.operations.SupportTicketCategorySpecifications;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import java.util.List;
import java.util.Set;
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
        categoryRepository.deleteRequiredPermissionMappings(savedEntity.getCategoryId());
        Set<String> requiredPermissionCodes = category.getRequiredPermissionCodes() == null
                ? Set.of() : category.getRequiredPermissionCodes();
        if (!requiredPermissionCodes.isEmpty()) {
            categoryRepository.insertRequiredPermissionMappings(savedEntity.getCategoryId(), requiredPermissionCodes);
        }
        return enrich(categoryPersistenceMapper.toDomain(savedEntity));
    }

    @Override
    public Optional<SupportTicketCategory> findById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .map(categoryPersistenceMapper::toDomain)
                .map(this::enrich);
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
                .map(this::enrich)
                .toList();
    }

    @Override
    public boolean existsActiveByCode(String code) {
        return categoryRepository.existsByCode(code);
    }

    @Override
    public boolean existsActiveByCodeAndCategoryIdNot(String code, UUID categoryId) {
        return categoryRepository.existsByCodeAndCategoryIdNot(code, categoryId);
    }

    @Override
    public boolean hasUnfinishedTickets(UUID categoryId) {
        return supportTicketRepository.existsByCategoryIdAndStatusIn(
                categoryId,
                List.of(
                        com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus.OPEN,
                        com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus.IN_PROGRESS,
                        com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus.RESOLVED
                )
        );
    }

    @Override
    public boolean hasTickets(UUID categoryId) {
        return supportTicketRepository.existsByCategoryId(categoryId);
    }

    private SupportTicketCategory enrich(SupportTicketCategory category) {
        category.setRequiredPermissionCodes(Set.copyOf(
                categoryRepository.findRequiredPermissionCodes(category.getCategoryId())
        ));
        return category;
    }
}
