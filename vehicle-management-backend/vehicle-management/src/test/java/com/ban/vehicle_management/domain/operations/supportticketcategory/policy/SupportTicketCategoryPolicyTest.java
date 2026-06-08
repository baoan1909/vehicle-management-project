package com.ban.vehicle_management.domain.operations.supportticketcategory.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class SupportTicketCategoryPolicyTest {

    private final SupportTicketCategoryPolicy categoryPolicy = new SupportTicketCategoryPolicy();

    @Test
    void shouldNormalizeFieldsAndSetDefaultsWhenInitialize() {
        SupportTicketCategory category = validCategory();
        category.setCode(" lost-card ");
        category.setName(" Mat the xe ");
        category.setDescription(" Khach mat the khi ra bai ");
        category.setStatus(null);

        categoryPolicy.initialize(category);

        assertEquals("LOST-CARD", category.getCode());
        assertEquals("Mat the xe", category.getName());
        assertEquals("Khach mat the khi ra bai", category.getDescription());
        assertEquals(SupportTicketCategoryStatus.ACTIVE, category.getStatus());
    }

    @Test
    void shouldKeepExistingStatusWhenInitialize() {
        SupportTicketCategory category = validCategory();
        category.setStatus(SupportTicketCategoryStatus.INACTIVE);

        categoryPolicy.initialize(category);

        assertEquals(SupportTicketCategoryStatus.INACTIVE, category.getStatus());
    }

    @Test
    void shouldRejectNullCategory() {
        assertThrows(BadRequestException.class, () -> categoryPolicy.initialize(null));
    }

    @Test
    void shouldRejectBlankCode() {
        SupportTicketCategory category = validCategory();
        category.setCode(" ");

        assertThrows(BadRequestException.class, () -> categoryPolicy.initialize(category));
    }

    @Test
    void shouldRejectCodeWithUnsupportedCharacters() {
        SupportTicketCategory category = validCategory();
        category.setCode("LOST CARD!");

        assertThrows(BadRequestException.class, () -> categoryPolicy.initialize(category));
    }

    @Test
    void shouldRejectBlankName() {
        SupportTicketCategory category = validCategory();
        category.setName(" ");

        assertThrows(BadRequestException.class, () -> categoryPolicy.initialize(category));
    }

    @Test
    void shouldRejectNameExceedingSchemaLength() {
        SupportTicketCategory category = validCategory();
        category.setName("A".repeat(151));

        assertThrows(BadRequestException.class, () -> categoryPolicy.initialize(category));
    }

    @Test
    void shouldRejectNullPriority() {
        SupportTicketCategory category = validCategory();
        category.setPriority(null);

        assertThrows(BadRequestException.class, () -> categoryPolicy.initialize(category));
    }

    @Test
    void shouldActivateCategory() {
        SupportTicketCategory category = validCategory();
        category.setStatus(SupportTicketCategoryStatus.INACTIVE);

        categoryPolicy.activate(category);

        assertEquals(SupportTicketCategoryStatus.ACTIVE, category.getStatus());
    }

    @Test
    void shouldDeactivateCategory() {
        SupportTicketCategory category = validCategory();

        categoryPolicy.deactivate(category);

        assertEquals(SupportTicketCategoryStatus.INACTIVE, category.getStatus());
    }

    private SupportTicketCategory validCategory() {
        SupportTicketCategory category = new SupportTicketCategory();
        category.setCode("LOST_CARD");
        category.setName("Mat the xe");
        category.setDescription("Khach mat the khi ra bai");
        category.setPriority(SupportTicketCategoryPriority.HIGH);
        category.setStatus(SupportTicketCategoryStatus.ACTIVE);
        return category;
    }
}
