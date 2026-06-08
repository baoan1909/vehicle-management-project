package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.infrastructure.mapper.operations.SupportTicketCategoryPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketCategoryEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketCategoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketRepository;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
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
class SupportTicketCategoryPersistenceAdapterTest {

    @Mock
    private SupportTicketCategoryRepository categoryRepository;

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private SupportTicketCategoryPersistenceMapper categoryPersistenceMapper;

    @InjectMocks
    private SupportTicketCategoryPersistenceAdapter categoryPersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingCategory() {
        SupportTicketCategory category = new SupportTicketCategory();
        category.setCategoryId(UUID.randomUUID());

        SupportTicketCategoryEntity entity = new SupportTicketCategoryEntity();

        when(categoryPersistenceMapper.toEntity(category)).thenReturn(entity);
        when(categoryRepository.saveAndFlush(entity)).thenReturn(entity);
        when(categoryPersistenceMapper.toDomain(entity)).thenReturn(category);

        SupportTicketCategory savedCategory = categoryPersistenceAdapter.save(category);

        assertEquals(category, savedCategory);
        verify(categoryRepository).saveAndFlush(entity);
    }

    @Test
    void shouldMapEntityWhenFindingById() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategoryEntity entity = new SupportTicketCategoryEntity();
        SupportTicketCategory category = new SupportTicketCategory();
        category.setCategoryId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(entity));
        when(categoryPersistenceMapper.toDomain(entity)).thenReturn(category);

        Optional<SupportTicketCategory> result = categoryPersistenceAdapter.findById(categoryId);

        assertTrue(result.isPresent());
        assertEquals(categoryId, result.get().getCategoryId());
    }

    @Test
    void shouldReturnMappedListWhenFindingAll() {
        SupportTicketCategoryEntity firstEntity = new SupportTicketCategoryEntity();
        SupportTicketCategoryEntity secondEntity = new SupportTicketCategoryEntity();
        SupportTicketCategory firstCategory = new SupportTicketCategory();
        SupportTicketCategory secondCategory = new SupportTicketCategory();

        when(categoryRepository.findAll(any(Specification.class))).thenReturn(List.of(firstEntity, secondEntity));
        when(categoryPersistenceMapper.toDomain(firstEntity)).thenReturn(firstCategory);
        when(categoryPersistenceMapper.toDomain(secondEntity)).thenReturn(secondCategory);

        List<SupportTicketCategory> result = categoryPersistenceAdapter.findAll(
                SupportTicketCategoryStatus.ACTIVE,
                SupportTicketCategoryPriority.HIGH,
                "CARD"
        );

        assertEquals(2, result.size());
        assertEquals(firstCategory, result.get(0));
        assertEquals(secondCategory, result.get(1));
    }

    @Test
    void shouldDelegateExistsActiveByCode() {
        when(categoryRepository.existsByCodeAndStatus("LOST_CARD", SupportTicketCategoryStatus.ACTIVE))
                .thenReturn(true);

        boolean exists = categoryPersistenceAdapter.existsActiveByCode("LOST_CARD");

        assertTrue(exists);
        verify(categoryRepository).existsByCodeAndStatus("LOST_CARD", SupportTicketCategoryStatus.ACTIVE);
    }

    @Test
    void shouldDelegateExistsActiveByCodeAndCategoryIdNot() {
        UUID categoryId = UUID.randomUUID();

        when(categoryRepository.existsByCodeAndStatusAndCategoryIdNot(
                "LOST_CARD",
                SupportTicketCategoryStatus.ACTIVE,
                categoryId
        )).thenReturn(true);

        boolean exists = categoryPersistenceAdapter.existsActiveByCodeAndCategoryIdNot("LOST_CARD", categoryId);

        assertTrue(exists);
        verify(categoryRepository).existsByCodeAndStatusAndCategoryIdNot(
                "LOST_CARD",
                SupportTicketCategoryStatus.ACTIVE,
                categoryId
        );
    }

    @Test
    void shouldCheckUnfinishedTickets() {
        UUID categoryId = UUID.randomUUID();
        List<SupportTicketStatus> unfinishedStatuses = List.of(
                SupportTicketStatus.OPEN,
                SupportTicketStatus.IN_PROGRESS,
                SupportTicketStatus.RESOLVED
        );

        when(supportTicketRepository.existsByCategoryIdAndStatusIn(categoryId, unfinishedStatuses))
                .thenReturn(true);

        boolean hasUnfinishedTickets = categoryPersistenceAdapter.hasUnfinishedTickets(categoryId);

        assertTrue(hasUnfinishedTickets);
        verify(supportTicketRepository).existsByCategoryIdAndStatusIn(categoryId, unfinishedStatuses);
    }
}
