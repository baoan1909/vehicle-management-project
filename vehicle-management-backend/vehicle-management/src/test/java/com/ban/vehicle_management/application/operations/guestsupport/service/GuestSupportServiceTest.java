package com.ban.vehicle_management.application.operations.guestsupport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.GuestSupportRequestPortOut;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.domain.operations.guestsupport.model.GuestSupportRequest;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request.CreateGuestSupportRequest;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.exception.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestSupportServiceTest {
    @Mock GuestSupportRequestPortOut requestPort;
    @Mock SupportTicketCategoryPortIn categoryPort;
    @Mock AuditLogPortOut auditPort;
    private GuestSupportService service;
    private GuestSupportProperties properties;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        properties = new GuestSupportProperties();
        properties.setCapabilitySecret("unit-test-secret-with-at-least-32-bytes");
        GuestSupportTokenService tokenService = new GuestSupportTokenService(properties);
        service = new GuestSupportService(requestPort, categoryPort, auditPort, tokenService, properties);
        categoryId = UUID.randomUUID();
        SupportTicketCategory category = new SupportTicketCategory();
        category.setCategoryId(categoryId);
        category.setStatus(SupportTicketCategoryStatus.ACTIVE);
        lenient().when(categoryPort.getCategoryById(categoryId)).thenReturn(category);
        lenient().when(requestPort.save(any())).thenAnswer(invocation -> {
            GuestSupportRequest request = invocation.getArgument(0);
            request.setCreatedAt(Instant.now());
            return request;
        });
    }

    @Test
    void createsRequestAndReturnsVerifiableCapability() {
        GuestSupportService.CreatedRequest created = service.create(validRequest(), "idem-1", "127.0.0.1");

        assertFalse(created.replayed());
        assertEquals(categoryId, created.request().getCategoryId());
        assertEquals(created.request().getRequestId().toString().length(), 36);
        when(requestPort.findByPublicReferenceCode(created.request().getPublicReferenceCode()))
                .thenReturn(Optional.of(created.request()));
        assertEquals(created.request().getRequestId(), service.access(
                created.request().getPublicReferenceCode(), created.capabilityToken()).getRequestId());
    }

    @Test
    void replaysSameIdempotencyKeyWithoutSecondInsert() {
        GuestSupportService.CreatedRequest first = service.create(validRequest(), "idem-2", "127.0.0.1");
        when(requestPort.findByContactHashAndIdempotencyHash(
                first.request().getContactHash(), first.request().getIdempotencyHash()))
                .thenReturn(Optional.of(first.request()));

        GuestSupportService.CreatedRequest replay = service.create(validRequest(), "idem-2", "127.0.0.1");

        assertEquals(first.capabilityToken(), replay.capabilityToken());
        assertEquals(true, replay.replayed());
        verify(requestPort).save(any());
    }

    @Test
    void rejectsInvalidContactBeforePersistence() {
        CreateGuestSupportRequest invalid = new CreateGuestSupportRequest(
                categoryId, "Help", "Content", "bad", null, "session");
        assertThrows(BadRequestException.class, () -> service.create(invalid, "idem-3", "127.0.0.1"));
        verify(requestPort, never()).save(any());
    }

    @Test
    void appliesIpRateLimit() {
        properties.setMaxRequestsPerIp(1);
        when(requestPort.countByIpHashSince(any(), any())).thenReturn(1L);
        assertThrows(TooManyRequestsException.class,
                () -> service.create(validRequest(), "idem-4", "127.0.0.1"));
        verify(requestPort, never()).save(any());
    }

    @Test
    void wrongOrExpiredCapabilityCannotReadRequest() {
        GuestSupportService.CreatedRequest created = service.create(validRequest(), "idem-5", "127.0.0.1");
        when(requestPort.findByPublicReferenceCode(created.request().getPublicReferenceCode()))
                .thenReturn(Optional.of(created.request()));
        assertThrows(NotFoundException.class, () -> service.access(created.request().getPublicReferenceCode(), "wrong"));

        created.request().setCapabilityExpiresAt(Instant.now().minus(Duration.ofSeconds(1)));
        assertThrows(NotFoundException.class, () -> service.access(
                created.request().getPublicReferenceCode(), created.capabilityToken()));
    }

    private CreateGuestSupportRequest validRequest() {
        return new CreateGuestSupportRequest(categoryId, "Need help", "How do I register?",
                "guest@example.com", null, "session-1");
    }
}
