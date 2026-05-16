package com.ban.vehicle_management.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

class AuditorAwareImplTest {

    @Test
    void shouldImplementUuidAuditorAware() {
        AuditorAware<UUID> auditorAware = new AuditorAwareImpl();

        assertTrue(auditorAware.getCurrentAuditor().isEmpty());
    }
}
