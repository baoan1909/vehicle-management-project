package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.ai.port.out.AiRuntimeModePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.model.AiRuntimeState;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AiRuntimeModeServiceTest {
    @Mock AiRuntimeModePortOut store;
    @Mock CurrentAccountPortIn accounts;
    @Mock CurrentAccountAccess actor;
    @InjectMocks AiRuntimeModeService service;
    private AiRuntimeState state(AiRuntimeMode mode,long epoch,String status) {
        return new AiRuntimeState(mode,mode,epoch,status,null,Instant.now());
    }
    @Test void requiresSystemAdminBeyondManagementPermission() {
        when(accounts.getCurrentAccountOrThrow()).thenReturn(actor);when(actor.roleCode()).thenReturn("partner_admin");
        assertThrows(AccessDeniedException.class,()->service.changeMode(AiRuntimeMode.QWEN_LOCAL,1));
        verify(accounts).requirePermission("AI_MODEL_MANAGE_ALL");verifyNoInteractions(store);
    }
    @Test void authorizedChangePersistsActorAndOnlyTwoModesExist() {
        UUID id=UUID.randomUUID();when(accounts.getCurrentAccountOrThrow()).thenReturn(actor);
        when(actor.roleCode()).thenReturn("system_admin");when(actor.accountId()).thenReturn(id);
        service.changeMode(AiRuntimeMode.QWEN_LOCAL,1);
        verify(store).requestSwitch(AiRuntimeMode.QWEN_LOCAL,1,id);assertEquals(2,AiRuntimeMode.values().length);
    }
    @Test void rejectsLateResultAfterEpochChangeAndRestoresThreadState() {
        when(store.current()).thenReturn(state(AiRuntimeMode.GOOGLE_API,1,"READY"));
        try(var scope=service.openScope()) {
            when(store.current()).thenReturn(state(AiRuntimeMode.QWEN_LOCAL,2,"READY"));
            assertThrows(AiRuntimeModeChangedException.class,service::assertCurrent);
        }
        assertDoesNotThrow(service::assertCurrent);
    }
    @Test void switchingPermitsNeitherProvider() {
        when(store.current()).thenReturn(state(AiRuntimeMode.GOOGLE_API,2,"SWITCHING"));
        assertFalse(service.allows(AiProvider.GEMINI));assertFalse(service.allows(AiProvider.LM_STUDIO));
        assertThrows(AiRuntimeModeChangedException.class,service::openScope);
    }
    @Test void localModePreservesExistingDataOptInAndGlobalDisable() {
        var properties=new AiAssistantProperties();
        properties.setAssistantEnabled(true);properties.setDataMode(AiDataMode.UNPAID);
        assertFalse(service.callsAllowed(properties));
        properties.setAllowUnpaidSupportData(true);assertTrue(service.callsAllowed(properties));
        properties.setAssistantEnabled(false);assertFalse(service.callsAllowed(properties));
    }
}
