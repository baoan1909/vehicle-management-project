package com.ban.vehicle_management.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

class JpaConfigTest {

    @Test
    void shouldEnableJpaAuditingWithAuditorAwareBean() {
        EnableJpaAuditing enableJpaAuditing = JpaConfig.class.getAnnotation(EnableJpaAuditing.class);

        assertNotNull(enableJpaAuditing);
        assertEquals("auditorAwareImpl", enableJpaAuditing.auditorAwareRef());
    }
}
