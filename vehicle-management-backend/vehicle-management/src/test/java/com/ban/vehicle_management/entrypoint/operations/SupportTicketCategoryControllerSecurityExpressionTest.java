package com.ban.vehicle_management.entrypoint.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.entrypoint.controller.operations.SupportTicketCategoryController;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request.CreateSupportTicketCategoryRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request.SupportTicketCategoryFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request.UpdateSupportTicketCategoryRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class SupportTicketCategoryControllerSecurityExpressionTest {

    @Test
    void adminCategoryEndpointsRequirePermissions() throws Exception {
        assertPermission("createCategory", new Class<?>[]{CreateSupportTicketCategoryRequest.class},
                "@permissionAuthorizer.hasPermission('SUPPORT_TICKET_CATEGORY_CREATE_ALL')");
        assertPermission("getCategoryById", new Class<?>[]{UUID.class},
                "@permissionAuthorizer.hasPermission('SUPPORT_TICKET_CATEGORY_READ_ALL')");
        assertPermission("getCategories", new Class<?>[]{SupportTicketCategoryFilterRequest.class},
                "@permissionAuthorizer.hasPermission('SUPPORT_TICKET_CATEGORY_READ_ALL')");
        assertPermission("updateCategory", new Class<?>[]{UUID.class, UpdateSupportTicketCategoryRequest.class},
                "@permissionAuthorizer.hasPermission('SUPPORT_TICKET_CATEGORY_UPDATE_ALL')");
        assertPermission("deleteCategory", new Class<?>[]{UUID.class},
                "@permissionAuthorizer.hasPermission('SUPPORT_TICKET_CATEGORY_ACTIVATE_ALL')");
        assertPermission("activateCategory", new Class<?>[]{UUID.class},
                "@permissionAuthorizer.hasPermission('SUPPORT_TICKET_CATEGORY_ACTIVATE_ALL')");
    }

    private void assertPermission(String method, Class<?>[] parameterTypes, String expected) throws Exception {
        PreAuthorize annotation = SupportTicketCategoryController.class
                .getMethod(method, parameterTypes).getAnnotation(PreAuthorize.class);
        assertEquals(expected, annotation.value());
    }
}
