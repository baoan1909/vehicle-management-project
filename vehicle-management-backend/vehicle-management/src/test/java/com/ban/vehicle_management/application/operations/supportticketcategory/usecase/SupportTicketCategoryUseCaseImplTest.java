package com.ban.vehicle_management.application.operations.supportticketcategory.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.supportticketcategory.port.out.SupportTicketCategoryPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportTicketCategoryUseCaseImplTest {

    @Mock
    private SupportTicketCategoryPortOut categoryPortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks
    private SupportTicketCategoryUseCaseImpl categoryUseCase;

    @Test
    void shouldCreateCategoryWhenValid() {
        SupportTicketCategory request = validCategory();
        request.setCode(" lost-card ");
        request.setStatus(null);

        when(categoryPortOut.existsActiveByCode("LOST-CARD")).thenReturn(false);
        when(categoryPortOut.save(any(SupportTicketCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicketCategory createdCategory = categoryUseCase.createCategory(request);

        assertNotNull(createdCategory.getCategoryId());
        assertEquals("LOST-CARD", createdCategory.getCode());
        assertEquals(SupportTicketCategoryStatus.ACTIVE, createdCategory.getStatus());
    }

    @Test
    void shouldRejectCreateWhenActiveCodeAlreadyExists() {
        SupportTicketCategory request = validCategory();

        when(categoryPortOut.existsActiveByCode("LOST_CARD")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryUseCase.createCategory(request));
        verify(categoryPortOut, never()).save(any(SupportTicketCategory.class));
    }

    @Test
    void shouldReturnCategoryById() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategory existingCategory = validCategory();
        existingCategory.setCategoryId(categoryId);

        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.of(existingCategory));

        SupportTicketCategory result = categoryUseCase.getCategoryById(categoryId);

        assertEquals(categoryId, result.getCategoryId());
    }

    @Test
    void shouldThrowWhenCategoryDoesNotExist() {
        UUID categoryId = UUID.randomUUID();

        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryUseCase.getCategoryById(categoryId));
    }

    @Test
    void shouldReturnFilteredCategoriesWithTrimmedKeyword() {
        when(categoryPortOut.findAll(
                SupportTicketCategoryStatus.ACTIVE,
                SupportTicketCategoryPriority.HIGH,
                "CARD"
        )).thenReturn(List.of(new SupportTicketCategory(), new SupportTicketCategory()));

        List<SupportTicketCategory> categories = categoryUseCase.getCategories(
                SupportTicketCategoryStatus.ACTIVE,
                SupportTicketCategoryPriority.HIGH,
                " CARD "
        );

        assertEquals(2, categories.size());
        verify(categoryPortOut).findAll(
                SupportTicketCategoryStatus.ACTIVE,
                SupportTicketCategoryPriority.HIGH,
                "CARD"
        );
    }

    @Test
    void shouldResolveCategoryAudienceFromEffectivePermissionInsteadOfRoleCode() {
        SupportTicketCategory customerCategory = validCategory();
        customerCategory.setAudienceType(com.ban.vehicle_management.shared.enumeration.operations.SupportTicketAudienceType.CUSTOMER);
        customerCategory.setDisplayOrder(10);
        customerCategory.setRequiredPermissionCodes(Set.of());
        CurrentAccountAccess access = org.mockito.Mockito.mock(CurrentAccountAccess.class);
        when(access.getEffectivePermissionCodes()).thenReturn(Set.of(
                "SUPPORT_TICKET_CREATE_OWN",
                "SUPPORT_TICKET_CATEGORY_CUSTOMER_OWN"
        ));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access);
        when(categoryPortOut.findAll(SupportTicketCategoryStatus.ACTIVE, null, null))
                .thenReturn(List.of(customerCategory));

        List<SupportTicketCategory> categories = categoryUseCase.getAvailableCategories();

        assertEquals(List.of(customerCategory), categories);
        verify(currentAccountPortIn).requirePermission("SUPPORT_TICKET_CREATE_OWN");
        verify(access, never()).roleCode();
    }

    @Test
    void shouldHideInternalCategoryWithoutAudiencePermission() {
        SupportTicketCategory internalCategory = validCategory();
        internalCategory.setAudienceType(com.ban.vehicle_management.shared.enumeration.operations.SupportTicketAudienceType.INTERNAL);
        internalCategory.setDisplayOrder(10);
        internalCategory.setRequiredPermissionCodes(Set.of());
        CurrentAccountAccess access = org.mockito.Mockito.mock(CurrentAccountAccess.class);
        when(access.getEffectivePermissionCodes()).thenReturn(Set.of("SUPPORT_TICKET_CREATE_OWN"));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access);
        when(categoryPortOut.findAll(SupportTicketCategoryStatus.ACTIVE, null, null))
                .thenReturn(List.of(internalCategory));

        assertEquals(List.of(), categoryUseCase.getAvailableCategories());
    }

    @Test
    void shouldApplyAnyAndAllPermissionMatchingForInternalCategories() {
        SupportTicketCategory anyCategory = validCategory();
        anyCategory.setAudienceType(com.ban.vehicle_management.shared.enumeration.operations.SupportTicketAudienceType.INTERNAL);
        anyCategory.setRequiredPermissionCodes(Set.of("DEVICE_READ_ALL", "PARKING_LOT_READ_ALL"));
        anyCategory.setPermissionMatchMode(com.ban.vehicle_management.shared.enumeration.operations.PermissionMatchMode.ANY);
        anyCategory.setDisplayOrder(10);
        SupportTicketCategory allCategory = validCategory();
        allCategory.setAudienceType(com.ban.vehicle_management.shared.enumeration.operations.SupportTicketAudienceType.INTERNAL);
        allCategory.setRequiredPermissionCodes(Set.of("DEVICE_READ_ALL", "PARKING_LOT_READ_ALL"));
        allCategory.setPermissionMatchMode(com.ban.vehicle_management.shared.enumeration.operations.PermissionMatchMode.ALL);
        allCategory.setDisplayOrder(20);
        CurrentAccountAccess access = org.mockito.Mockito.mock(CurrentAccountAccess.class);
        when(access.getEffectivePermissionCodes()).thenReturn(Set.of(
                "SUPPORT_TICKET_CREATE_OWN",
                "SUPPORT_TICKET_CATEGORY_INTERNAL_OWN",
                "DEVICE_READ_ALL"
        ));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access);
        when(categoryPortOut.findAll(SupportTicketCategoryStatus.ACTIVE, null, null))
                .thenReturn(List.of(allCategory, anyCategory));

        assertEquals(List.of(anyCategory), categoryUseCase.getAvailableCategories());
    }

    @Test
    void shouldUpdateCategoryWhenValid() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategory existingCategory = validCategory();
        existingCategory.setCategoryId(categoryId);

        SupportTicketCategory request = new SupportTicketCategory();
        request.setCode(" wrong-fee ");
        request.setName(" Khieu nai phi ");
        request.setDescription(" Khach bi tinh sai phi ");
        request.setPriority(SupportTicketCategoryPriority.URGENT);
        request.setAudienceType(existingCategory.getAudienceType());
        request.setPermissionMatchMode(existingCategory.getPermissionMatchMode());
        request.setDisplayOrder(existingCategory.getDisplayOrder());
        request.setRequiredPermissionCodes(Set.of());

        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryPortOut.existsActiveByCodeAndCategoryIdNot("WRONG-FEE", categoryId)).thenReturn(false);
        when(categoryPortOut.save(any(SupportTicketCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicketCategory updatedCategory = categoryUseCase.updateCategory(categoryId, request);

        assertEquals("WRONG-FEE", updatedCategory.getCode());
        assertEquals("Khieu nai phi", updatedCategory.getName());
        assertEquals("Khach bi tinh sai phi", updatedCategory.getDescription());
        assertEquals(SupportTicketCategoryPriority.URGENT, updatedCategory.getPriority());
        assertEquals(SupportTicketCategoryStatus.ACTIVE, updatedCategory.getStatus());
    }

    @Test
    void shouldRejectUpdateWhenActiveCodeAlreadyExists() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategory existingCategory = validCategory();
        existingCategory.setCategoryId(categoryId);

        SupportTicketCategory request = validCategory();
        request.setCode("WRONG_FEE");

        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryPortOut.existsActiveByCodeAndCategoryIdNot("WRONG_FEE", categoryId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryUseCase.updateCategory(categoryId, request));
        verify(categoryPortOut, never()).save(any(SupportTicketCategory.class));
    }

    @Test
    void shouldRejectCodeOrAudienceChangeAfterCategoryWasUsed() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategory existingCategory = validCategory();
        existingCategory.setCategoryId(categoryId);
        SupportTicketCategory request = validCategory();
        request.setCode("NEW_CODE");
        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryPortOut.hasTickets(categoryId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryUseCase.updateCategory(categoryId, request));
        verify(categoryPortOut, never()).save(any(SupportTicketCategory.class));
    }

    @Test
    void shouldDeactivateCategoryOnDelete() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategory existingCategory = validCategory();
        existingCategory.setCategoryId(categoryId);

        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.of(existingCategory));

        categoryUseCase.deleteCategory(categoryId);

        assertEquals(SupportTicketCategoryStatus.INACTIVE, existingCategory.getStatus());
        verify(categoryPortOut).save(existingCategory);
    }

    @Test
    void shouldDoNothingWhenDeletingInactiveCategory() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategory existingCategory = validCategory();
        existingCategory.setCategoryId(categoryId);
        existingCategory.setStatus(SupportTicketCategoryStatus.INACTIVE);

        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.of(existingCategory));

        categoryUseCase.deleteCategory(categoryId);

        verify(categoryPortOut, never()).save(any(SupportTicketCategory.class));
    }

    @Test
    void shouldDeactivateCategoryEvenWhenItHasUnfinishedTickets() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategory existingCategory = validCategory();
        existingCategory.setCategoryId(categoryId);

        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        categoryUseCase.deleteCategory(categoryId);

        assertEquals(SupportTicketCategoryStatus.INACTIVE, existingCategory.getStatus());
        verify(categoryPortOut).save(existingCategory);
    }

    @Test
    void shouldActivateInactiveCategory() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategory existingCategory = validCategory();
        existingCategory.setCategoryId(categoryId);
        existingCategory.setStatus(SupportTicketCategoryStatus.INACTIVE);

        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryPortOut.save(any(SupportTicketCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicketCategory activatedCategory = categoryUseCase.activateCategory(categoryId);

        assertEquals(SupportTicketCategoryStatus.ACTIVE, activatedCategory.getStatus());
    }

    @Test
    void shouldReturnExistingCategoryWhenAlreadyActive() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategory existingCategory = validCategory();
        existingCategory.setCategoryId(categoryId);

        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.of(existingCategory));

        SupportTicketCategory result = categoryUseCase.activateCategory(categoryId);

        assertEquals(SupportTicketCategoryStatus.ACTIVE, result.getStatus());
        verify(categoryPortOut, never()).save(any(SupportTicketCategory.class));
    }

    @Test
    void shouldActivateCategoryWithoutRecheckingGloballyUniqueCode() {
        UUID categoryId = UUID.randomUUID();
        SupportTicketCategory existingCategory = validCategory();
        existingCategory.setCategoryId(categoryId);
        existingCategory.setStatus(SupportTicketCategoryStatus.INACTIVE);

        when(categoryPortOut.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryPortOut.save(any(SupportTicketCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicketCategory result = categoryUseCase.activateCategory(categoryId);

        assertEquals(SupportTicketCategoryStatus.ACTIVE, result.getStatus());
        verify(categoryPortOut).save(existingCategory);
    }

    private SupportTicketCategory validCategory() {
        SupportTicketCategory category = new SupportTicketCategory();
        category.setCode("LOST_CARD");
        category.setName("Mat the xe");
        category.setDescription("Khach mat the khi ra bai");
        category.setPriority(SupportTicketCategoryPriority.HIGH);
        category.setStatus(SupportTicketCategoryStatus.ACTIVE);
        category.setAudienceType(com.ban.vehicle_management.shared.enumeration.operations.SupportTicketAudienceType.CUSTOMER);
        category.setPermissionMatchMode(com.ban.vehicle_management.shared.enumeration.operations.PermissionMatchMode.ANY);
        category.setDisplayOrder(0);
        category.setRequiredPermissionCodes(Set.of());
        return category;
    }
}
