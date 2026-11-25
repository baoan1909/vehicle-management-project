package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class LocalAiModelRegistrationServiceTest {
    @Mock AiModelConfigurationPortOut configurations;
    @Mock CurrentAccountPortIn accounts;
    @Mock CurrentAccountAccess actor;
    @InjectMocks LocalAiModelRegistrationService service;
    private AiProviderModel model(String id,String action) {
        return new AiProviderModel(AiProvider.LM_STUDIO,id,id,"configured-local",2048,512,List.of(action));
    }
    @Test void configuredLocalModelsAreCandidatesAndExistingGoogleIsPreserved() {
        when(accounts.getCurrentAccountOrThrow()).thenReturn(actor);when(actor.roleCode()).thenReturn("system_admin");
        UUID account=UUID.randomUUID();when(actor.accountId()).thenReturn(account);
        var google=new AiModelConfiguration();google.setProvider(AiProvider.GEMINI);google.setModelId("google-existing");google.setStatus(AiModelStatus.ACTIVE);
        when(configurations.findAll()).thenReturn(List.of(google));
        service.registerCandidates(List.of(model("qwen3.5-4b","generateContent"),model("text-embedding-qwen3-embedding-0.6b","embedContent")));
        var saved=ArgumentCaptor.forClass(AiModelConfiguration.class);verify(configurations,times(2)).save(saved.capture());
        assertTrue(saved.getAllValues().stream().allMatch(c->c.getProvider()==AiProvider.LM_STUDIO&&c.getStatus()==AiModelStatus.CANDIDATE&&account.equals(c.getCreatedBy())));
        assertEquals(768,saved.getAllValues().stream().filter(c->c.getUseCase()==AiUseCase.EMBEDDING).findFirst().orElseThrow().getOutputDimension());
        assertEquals(AiModelStatus.ACTIVE,google.getStatus());verify(accounts).requirePermission("AI_MODEL_MANAGE_ALL");
    }
    @Test void permissionAloneCannotRegisterLocalModelsForPartnerAdmin() {
        when(accounts.getCurrentAccountOrThrow()).thenReturn(actor);when(actor.roleCode()).thenReturn("partner_admin");
        assertThrows(AccessDeniedException.class,()->service.registerCandidates(List.of(model("qwen3.5-4b","generateContent"))));
        verifyNoInteractions(configurations);
    }
}
