package com.ban.vehicle_management.application.ai.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.ai.port.out.AiToolCallPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.*;
import com.ban.vehicle_management.domain.ai.model.AiToolCall;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
@ExtendWith(MockitoExtension.class)
class AiToolCallUseCaseImplTest {
    @Mock AiToolCallPortOut calls;
    @Mock CurrentAccountPortIn account;
    @Mock AiToolExecutionService executor;
    @Mock ChatConversationPortOut chat;
    @Mock ChatRealtimeEventPublisherPortOut realtime;
    @Mock ChatRealtimeEventMapper events;
    private AiToolCallUseCaseImpl service;
    private AiToolCall call;
    private UUID actor;
    @BeforeEach void arrange() {
        service = new AiToolCallUseCaseImpl(calls, account, executor, chat, realtime, events, new ObjectMapper());
        actor = UUID.randomUUID(); call = new AiToolCall();
        call.setToolCallId(UUID.randomUUID()); call.setRequestedBy(actor);
        call.setToolType(AiToolType.WRITE); call.setToolName("create_support_ticket");
        call.setStatus(AiToolCallStatus.AWAITING_CONFIRMATION);
        call.setArgumentPayloadRedacted("{\"title\":\"Login issue\",\"categoryId\":\"frozen-category\"}");
        call.setIdempotencyKey("same-frozen-request"); call.setExpiresAt(Instant.now().plusSeconds(300));
        when(account.getCurrentAccountIdOrThrow()).thenReturn(actor);
        when(calls.findByIdForUpdate(call.getToolCallId())).thenAnswer(invocation -> Optional.of(call));
        lenient().when(calls.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    @Test void confirmedRetryExecutesOnceWithFrozenArgumentsAndSameIdempotencyKey() {
        when(executor.execute(eq("create_support_ticket"), any(), isNull(), eq("same-frozen-request")))
                .thenReturn(new AiToolExecutionService.ToolExecutionResult("create_support_ticket", "{}", false));
        assertEquals(AiToolCallStatus.SUCCEEDED, service.confirm(call.getToolCallId()).getStatus());
        assertEquals(AiToolCallStatus.SUCCEEDED, service.confirm(call.getToolCallId()).getStatus());
        var args = ArgumentCaptor.forClass(com.fasterxml.jackson.databind.JsonNode.class);
        verify(executor, times(1)).execute(eq("create_support_ticket"), args.capture(), isNull(), eq("same-frozen-request"));
        assertEquals("frozen-category", args.getValue().path("categoryId").asText());
        assertEquals(actor, call.getConfirmedBy());
    }
    @Test void deniedActionCannotLaterBeConfirmed() {
        assertEquals(AiToolCallStatus.DENIED, service.deny(call.getToolCallId()).getStatus());
        assertThrows(BadRequestException.class, () -> service.confirm(call.getToolCallId()));
        verifyNoInteractions(executor);
    }
    @Test void expiredConfirmationNeverExecutes() {
        call.setExpiresAt(Instant.EPOCH);
        assertEquals(AiToolCallStatus.EXPIRED, service.confirm(call.getToolCallId()).getStatus());
        assertEquals("CONFIRMATION_EXPIRED", call.getFailureCode()); verifyNoInteractions(executor);
    }
    @Test void anotherAccountCannotConfirmOrDeny() {
        when(account.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        assertThrows(AccessDeniedException.class, () -> service.confirm(call.getToolCallId()));
        assertThrows(AccessDeniedException.class, () -> service.deny(call.getToolCallId()));
        verifyNoInteractions(executor); verify(calls, never()).save(any());
    }
    @Test void executingRetryCannotExecuteAgain() {
        call.setStatus(AiToolCallStatus.EXECUTING);
        assertSame(call, service.confirm(call.getToolCallId())); verifyNoInteractions(executor);
    }
    @Test void revokedPermissionAtExecutionCannotBecomeSuccess() {
        when(executor.execute(anyString(), any(), isNull(), anyString())).thenThrow(new AccessDeniedException("revoked"));
        assertEquals(AiToolCallStatus.FAILED, service.confirm(call.getToolCallId()).getStatus());
        assertNull(call.getExecutedAt()); assertEquals("AccessDeniedException", call.getFailureCode());
        assertThrows(BadRequestException.class, () -> service.confirm(call.getToolCallId()));
        verify(executor, times(1)).execute(anyString(), any(), isNull(), anyString());
    }
}
