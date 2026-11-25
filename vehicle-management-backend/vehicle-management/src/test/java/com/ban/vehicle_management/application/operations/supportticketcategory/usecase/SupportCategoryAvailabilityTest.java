package com.ban.vehicle_management.application.operations.supportticketcategory.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.out.SupportTicketCategoryPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.shared.enumeration.operations.*;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SupportCategoryAvailabilityTest {
    @Mock SupportTicketCategoryPortOut categories;
    @Mock CurrentAccountPortIn accounts;
    @Mock CurrentAccountAccess access;
    @InjectMocks SupportTicketCategoryUseCaseImpl useCase;

    @ParameterizedTest
    @ValueSource(strings = {"LOST_CARD_REPORT_READ_ALL", "LOST_CARD_REPORT_CREATE_ALL", "LOST_CARD_REPORT_UPDATE_ALL"})
    void eachLostCardPermissionAllowsListingAndDirectUseWithAny(String permission) {
        SupportTicketCategory category = lostCardCategory();
        authenticate(Set.of("SUPPORT_TICKET_CATEGORY_INTERNAL_OWN", permission));
        when(categories.findAll(SupportTicketCategoryStatus.ACTIVE, null, null)).thenReturn(List.of(category));
        when(categories.findById(category.getCategoryId())).thenReturn(Optional.of(category));
        assertEquals(List.of(category), useCase.getAvailableCategories());
        assertSame(category, useCase.ensureAvailableCategory(category.getCategoryId()));
        verify(access, never()).roleCode();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPPORT_TICKET_CATEGORY_CUSTOMER_OWN", "SUPPORT_TICKET_CATEGORY_INTERNAL_OWN"})
    void lostCardWorkflowCannotBeListedOrForgedWithoutFeaturePermission(String audiencePermission) {
        SupportTicketCategory category = lostCardCategory();
        authenticate(Set.of(audiencePermission));
        when(categories.findAll(SupportTicketCategoryStatus.ACTIVE, null, null)).thenReturn(List.of(category));
        when(categories.findById(category.getCategoryId())).thenReturn(Optional.of(category));
        assertTrue(useCase.getAvailableCategories().isEmpty());
        assertThrows(AccessDeniedException.class, () -> useCase.ensureAvailableCategory(category.getCategoryId()));
    }

    @Test
    void inactiveAuthorizedCategoryReturnsBusinessConflict() {
        SupportTicketCategory category = lostCardCategory();
        category.setStatus(SupportTicketCategoryStatus.INACTIVE);
        authenticate(Set.of("SUPPORT_TICKET_CATEGORY_INTERNAL_OWN", "LOST_CARD_REPORT_READ_ALL"));
        when(categories.findById(category.getCategoryId())).thenReturn(Optional.of(category));
        assertThrows(ConflictException.class, () -> useCase.ensureAvailableCategory(category.getCategoryId()));
    }

    @Test
    void publicCategoriesExcludeInternalInactiveAndPermissionRestrictedCustomerCategories() {
        SupportTicketCategory internal = lostCardCategory();
        SupportTicketCategory customer = customerCategory();
        SupportTicketCategory restricted = customerCategory();
        restricted.setRequiredPermissionCodes(Set.of("ACCOUNT_READ_ALL"));
        SupportTicketCategory inactive = customerCategory();
        inactive.setStatus(SupportTicketCategoryStatus.INACTIVE);
        when(categories.findAll(SupportTicketCategoryStatus.ACTIVE, null, null))
                .thenReturn(List.of(internal, restricted, customer, inactive));
        assertEquals(List.of(customer), useCase.getPublicCategories());
        assertThrows(BadRequestException.class, () -> useCase.ensurePublicAvailableCategory(internal.getCategoryId()));
        verifyNoInteractions(accounts);
    }

    @Test
    void internalAudienceCannotForgeCustomerCategoryEvenWithBusinessPermission() {
        SupportTicketCategory category = customerCategory();
        authenticate(Set.of("SUPPORT_TICKET_CATEGORY_INTERNAL_OWN", "LOST_CARD_REPORT_READ_ALL"));
        when(categories.findById(category.getCategoryId())).thenReturn(Optional.of(category));
        assertThrows(AccessDeniedException.class, () -> useCase.ensureAvailableCategory(category.getCategoryId()));
    }

    private void authenticate(Set<String> permissions) {
        when(accounts.getCurrentAccountOrThrow()).thenReturn(access);
        when(access.getEffectivePermissionCodes()).thenReturn(permissions);
    }

    private SupportTicketCategory customerCategory() {
        SupportTicketCategory category = lostCardCategory();
        category.setAudienceType(SupportTicketAudienceType.CUSTOMER);
        category.setRequiredPermissionCodes(Set.of());
        return category;
    }

    private SupportTicketCategory lostCardCategory() {
        SupportTicketCategory category = new SupportTicketCategory();
        category.setCategoryId(UUID.randomUUID());
        category.setCode("INTERNAL_LOST_CARD_WORKFLOW");
        category.setAudienceType(SupportTicketAudienceType.INTERNAL);
        category.setStatus(SupportTicketCategoryStatus.ACTIVE);
        category.setPermissionMatchMode(PermissionMatchMode.ANY);
        category.setDisplayOrder(260);
        category.setRequiredPermissionCodes(Set.of("LOST_CARD_REPORT_READ_ALL", "LOST_CARD_REPORT_CREATE_ALL", "LOST_CARD_REPORT_UPDATE_ALL"));
        return category;
    }
}
