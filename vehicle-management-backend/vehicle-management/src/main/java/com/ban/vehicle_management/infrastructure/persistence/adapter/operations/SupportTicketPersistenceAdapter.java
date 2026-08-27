package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.infrastructure.mapper.operations.SupportTicketPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketCategoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.operations.SupportTicketSpecifications;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketPersistenceAdapter implements SupportTicketPortOut {

    private static final Set<String> ASSIGNABLE_ACCOUNT_PERMISSIONS = Set.of(
            "SUPPORT_TICKET_READ_ASSIGNED",
            "SUPPORT_TICKET_PROCESS_ASSIGNED",
            "SUPPORT_TICKET_RESPOND_ASSIGNED"
    );
    private static final List<SupportTicketStatus> ACTIVE_WORKFLOW_STATUSES = List.of(
            SupportTicketStatus.OPEN,
            SupportTicketStatus.IN_PROGRESS,
            SupportTicketStatus.RESOLVED
    );

    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketCategoryRepository categoryRepository;
    private final AccountAuthorizationPortOut accountAuthorizationPortOut;
    private final SupportTicketPersistenceMapper supportTicketPersistenceMapper;

    public SupportTicketPersistenceAdapter(
            SupportTicketRepository supportTicketRepository,
            SupportTicketCategoryRepository categoryRepository,
            AccountAuthorizationPortOut accountAuthorizationPortOut,
            SupportTicketPersistenceMapper supportTicketPersistenceMapper
    ) {
        this.supportTicketRepository = supportTicketRepository;
        this.categoryRepository = categoryRepository;
        this.accountAuthorizationPortOut = accountAuthorizationPortOut;
        this.supportTicketPersistenceMapper = supportTicketPersistenceMapper;
    }

    @Override
    public SupportTicket save(SupportTicket supportTicket) {
        SupportTicketEntity savedEntity = supportTicketRepository.saveAndFlush(
                supportTicketPersistenceMapper.toEntity(supportTicket)
        );
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<SupportTicket> findById(UUID supportTicketId) {
        return supportTicketRepository.findById(supportTicketId)
                .map(this::mapToDomain);
    }

    @Override
    public List<SupportTicket> findAll(
            UUID customerId,
            UUID categoryId,
            UUID assignedTo,
            SupportTicketStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    ) {
        return supportTicketRepository.findAll(
                        SupportTicketSpecifications.withFilters(customerId, categoryId, assignedTo, status, priority, keyword)
                )
                .stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public boolean existsActiveCategoryById(UUID categoryId) {
        return categoryRepository.existsByCategoryIdAndStatus(categoryId, SupportTicketCategoryStatus.ACTIVE);
    }

    @Override
    public boolean existsAssignableAccountById(UUID accountId) {
        return accountAuthorizationPortOut.findByAccountId(accountId)
                .filter(access -> access.canUseBusinessPermissions())
                .map(access -> access.getEffectivePermissionCodes().containsAll(ASSIGNABLE_ACCOUNT_PERMISSIONS))
                .orElse(false);
    }

    @Override
    public boolean existsActiveWorkflowByCustomerIdAndCategoryId(UUID customerId, UUID categoryId) {
        return supportTicketRepository.existsByCustomerIdAndCategoryIdAndStatusIn(
                customerId,
                categoryId,
                ACTIVE_WORKFLOW_STATUSES
        );
    }

    private SupportTicket mapToDomain(SupportTicketEntity entity) {
        SupportTicket supportTicket = supportTicketPersistenceMapper.toDomain(entity);
        if (supportTicket.getCategoryId() == null || supportTicket.getCategoryCode() != null) {
            return supportTicket;
        }

        categoryRepository.findById(supportTicket.getCategoryId())
                .ifPresent(category -> {
                    supportTicket.setCategoryCode(category.getCode());
                    supportTicket.setCategoryName(category.getName());
                    supportTicket.setPriority(category.getPriority());
                });

        return supportTicket;
    }
}
