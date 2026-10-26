package com.ban.vehicle_management.application.operations.guestsupport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.application.ai.service.AiAssistantProperties;
import com.ban.vehicle_management.application.ai.service.AiModelRouter;
import com.ban.vehicle_management.application.ai.service.KnowledgeRetrievalService;
import com.ban.vehicle_management.application.ai.service.PiiRedactionService;
import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalContext;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response.GuestAssistantResponse;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestPublicAssistantServiceTest {
    @Mock KnowledgeRetrievalService retrievalService;
    @Mock AiModelRouter modelRouter;
    @Mock AiProviderPortOut provider;
    private GuestPublicAssistantService service;

    @BeforeEach
    void setUp() {
        GuestSupportProperties guestProperties = new GuestSupportProperties();
        guestProperties.setCapabilitySecret("unit-test-secret-with-at-least-32-bytes");
        AiAssistantProperties assistantProperties = new AiAssistantProperties();
        assistantProperties.setAssistantEnabled(true);
        assistantProperties.setAllowUnpaidSupportData(true);
        when(provider.provider()).thenReturn(AiProvider.GEMINI);
        service = new GuestPublicAssistantService(
                retrievalService,
                modelRouter,
                List.of(provider),
                assistantProperties,
                new PiiRedactionService(),
                new GuestAssistantRateLimiter(guestProperties),
                new GuestSupportTokenService(guestProperties));
    }

    @Test
    void alwaysRetrievesPublicScopeAndDeclaresNoTools() {
        KnowledgeSearchResult evidence = new KnowledgeSearchResult(
                UUID.randomUUID(), UUID.randomUUID(), "Đăng ký", "Mở trang đăng ký.", null, 1, "Onboarding", BigDecimal.ONE);
        KnowledgeRetrievalResult retrieval = new KnowledgeRetrievalResult(
                null, UUID.randomUUID(), List.of(evidence), UUID.randomUUID(), BigDecimal.ONE,
                true, "dang ky", "v1");
        when(retrievalService.search(any(), eq("Cách đăng ký?"), eq(List.of("PUBLIC")), eq(5)))
                .thenReturn(retrieval);
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setProvider(AiProvider.GEMINI);
        when(modelRouter.resolveCandidates(eq(AiUseCase.SUPPORT_CHAT), any(), eq(null), eq(1)))
                .thenReturn(List.of(configuration));
        when(provider.generate(any(), eq(configuration)))
                .thenReturn(AiResponse.success("Bạn mở trang đăng ký [C1].", "{}", 10, 10));

        GuestAssistantResponse answer = service.answer("Cách đăng ký?", "session", "127.0.0.1");

        assertEquals("Bạn mở trang đăng ký [C1].", answer.answer());
        ArgumentCaptor<AiRequest> requestCaptor = ArgumentCaptor.forClass(AiRequest.class);
        verify(provider).generate(requestCaptor.capture(), eq(configuration));
        assertFalse(requestCaptor.getValue().tools().iterator().hasNext());
        verify(retrievalService).search(
                eq(KnowledgeRetrievalContext.global()), eq("Cách đăng ký?"), eq(List.of("PUBLIC")), eq(5));
    }
}
