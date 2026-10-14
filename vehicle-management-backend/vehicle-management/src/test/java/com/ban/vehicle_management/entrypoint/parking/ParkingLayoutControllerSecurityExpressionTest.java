package com.ban.vehicle_management.entrypoint.parking;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.entrypoint.controller.parking.ParkingLayoutController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.prepost.PreAuthorize;

class ParkingLayoutControllerSecurityExpressionTest {

    @Test
    void discardDraftShouldUseAValidConfigurePermissionExpression() throws Exception {
        PreAuthorize authorization = ParkingLayoutController.class
                .getMethod("discardDraft", UUID.class, Long.class)
                .getAnnotation(PreAuthorize.class);

        assertEquals(
                "@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')",
                authorization.value());
        assertDoesNotThrow(() -> new SpelExpressionParser().parseExpression(authorization.value()));
    }
}
