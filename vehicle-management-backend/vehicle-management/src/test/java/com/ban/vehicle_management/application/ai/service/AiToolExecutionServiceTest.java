package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.domain.ai.policy.AiToolRegistry;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiToolExecutionServiceTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private SupportTicketPortIn supportTicketPortIn;
    @Mock
    private SubscriptionPortIn subscriptionPortIn;
    @Mock
    private KnowledgeRetrievalService retrievalService;
    @Mock
    private KnowledgeAccessContextResolver accessContextResolver;

    private AiToolExecutionService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AiToolExecutionService(
                new AiToolRegistry(), currentAccountPortIn, supportTicketPortIn, subscriptionPortIn,
                retrievalService, accessContextResolver, objectMapper);
    }

    @Test
    void assistantWorkerShouldExecuteAuthorizedPersonalReadTool() throws Exception {
        when(supportTicketPortIn.getMyTickets(null, null)).thenReturn(List.of());

        AiToolExecutionService.ToolExecutionResult result = service.executeFromAssistantWorker(
                "list_my_support_tickets", objectMapper.readTree("{}"));

        assertTrue(result.responseJson().contains("[]"));
        assertTrue(result.sensitive());
        verify(currentAccountPortIn).requirePermission("SUPPORT_TICKET_READ_OWN");
        verify(supportTicketPortIn).getMyTickets(null, null);
    }

    @Test
    void assistantWorkerShouldNeverExecuteWriteToolWithoutConfirmationFlow() throws Exception {
        String arguments = """
                {"categoryId":"%s","title":"Cần hỗ trợ","content":"Nội dung cần hỗ trợ"}
                """.formatted(UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> service.executeFromAssistantWorker(
                "create_support_ticket", objectMapper.readTree(arguments)));

        verify(currentAccountPortIn).requirePermission("SUPPORT_TICKET_CREATE_OWN");
    }
}
