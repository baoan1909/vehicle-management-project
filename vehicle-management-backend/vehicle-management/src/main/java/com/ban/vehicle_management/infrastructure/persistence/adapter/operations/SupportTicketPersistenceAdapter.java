package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.infrastructure.mapper.operations.SupportTicketPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketCategoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.operations.SupportTicketSpecifications;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketPersistenceAdapter implements SupportTicketPortOut {

    private static final List<String> ASSIGNABLE_ROLE_CODES = List.of("EMPLOYEE", "PARKING_MANAGER");

    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketCategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final SupportTicketPersistenceMapper supportTicketPersistenceMapper;

    public SupportTicketPersistenceAdapter(
            SupportTicketRepository supportTicketRepository,
            SupportTicketCategoryRepository categoryRepository,
            AccountRepository accountRepository,
            SupportTicketPersistenceMapper supportTicketPersistenceMapper
    ) {
        this.supportTicketRepository = supportTicketRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.supportTicketPersistenceMapper = supportTicketPersistenceMapper;
    }

    @Override
    public SupportTicket save(SupportTicket supportTicket) {
        SupportTicketEntity savedEntity = supportTicketRepository.saveAndFlush(
                supportTicketPersistenceMapper.toEntity(supportTicket)
        );
        return supportTicketPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<SupportTicket> findById(UUID supportTicketId) {
        return supportTicketRepository.findById(supportTicketId)
                .map(supportTicketPersistenceMapper::toDomain);
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
                .map(supportTicketPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveCategoryById(UUID categoryId) {
        return categoryRepository.existsByCategoryIdAndStatus(categoryId, SupportTicketCategoryStatus.ACTIVE);
    }

    @Override
    public boolean existsAssignableAccountById(UUID accountId) {
        return accountRepository.existsAssignableSupportTicketAccount(
                accountId,
                AccountStatus.ACTIVE,
                ASSIGNABLE_ROLE_CODES
        );
    }
}