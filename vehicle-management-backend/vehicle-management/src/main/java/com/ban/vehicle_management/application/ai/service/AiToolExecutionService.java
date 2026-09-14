package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.ban.vehicle_management.domain.ai.policy.AiToolDefinition;
import com.ban.vehicle_management.domain.ai.policy.AiToolRegistry;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiToolExecutionService {

    private final AiToolRegistry toolRegistry;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final SupportTicketPortIn supportTicketPortIn;
    private final SubscriptionPortIn subscriptionPortIn;
    private final KnowledgeRetrievalPortOut knowledgeRetrievalPortOut;
    private final ObjectMapper objectMapper;

    public AiToolExecutionService(
            AiToolRegistry toolRegistry,
            CurrentAccountPortIn currentAccountPortIn,
            SupportTicketPortIn supportTicketPortIn,
            SubscriptionPortIn subscriptionPortIn,
            KnowledgeRetrievalPortOut knowledgeRetrievalPortOut,
            ObjectMapper objectMapper
    ) {
        this.toolRegistry = toolRegistry;
        this.currentAccountPortIn = currentAccountPortIn;
        this.supportTicketPortIn = supportTicketPortIn;
        this.subscriptionPortIn = subscriptionPortIn;
        this.knowledgeRetrievalPortOut = knowledgeRetrievalPortOut;
        this.objectMapper = objectMapper;
    }

    public AiToolDefinition validateToolCall(String toolName, JsonNode arguments) {
        AiToolDefinition definition = toolRegistry.require(toolName);
        currentAccountPortIn.requirePermission(definition.requiredPermission());
        definition.validate(arguments);
        return definition;
    }

    public ToolExecutionResult execute(String toolName, JsonNode arguments, UUID conversationId, String idempotencyKey) {
        AiToolDefinition definition = validateToolCall(toolName, arguments);
        Object result = switch (definition.name()) {
            case "search_support_knowledge" -> searchKnowledge(arguments);
            case "get_my_support_ticket" -> getMySupportTicket(arguments);
            case "list_my_support_tickets" -> listMySupportTickets(arguments);
            case "get_my_subscription_status" -> listMySubscriptions(arguments);
            case "create_support_ticket" -> createSupportTicket(arguments, conversationId, idempotencyKey);
            default -> throw new BadRequestException("AI tool is not executable");
        };
        return new ToolExecutionResult(definition.name(), toJson(result), false);
    }

    public ToolExecutionResult executeFromAssistantWorker(String toolName, JsonNode arguments) {
        AiToolDefinition definition = toolRegistry.require(toolName);
        definition.validate(arguments);
        Object result = switch (definition.name()) {
            case "search_support_knowledge" -> searchKnowledge(arguments);
            default -> throw new BadRequestException("AI tool requires user confirmation or request-time authorization");
        };
        return new ToolExecutionResult(definition.name(), toJson(result), false);
    }

    private Object searchKnowledge(JsonNode arguments) {
        String query = requiredText(arguments, "query");
        List<KnowledgeSearchResult> results = knowledgeRetrievalPortOut.search(
                null,
                query,
                List.of("PUBLIC", "CUSTOMER"),
                5
        );
        return Map.of(
                "responseText",
                results.isEmpty()
                        ? "Chưa có tài liệu hỗ trợ đủ để trả lời. Bạn có thể tạo phiếu hỗ trợ để nhân viên xử lý."
                        : "Đã tìm thấy " + results.size() + " tài liệu hỗ trợ phù hợp.",
                "citations",
                results
        );
    }

    private Object getMySupportTicket(JsonNode arguments) {
        return supportTicketPortIn.getTicketById(UUID.fromString(requiredText(arguments, "supportTicketId")));
    }

    private Object listMySupportTickets(JsonNode arguments) {
        SupportTicketStatus status = optionalEnum(arguments, "status", SupportTicketStatus.class);
        String keyword = optionalText(arguments, "keyword");
        return supportTicketPortIn.getMyTickets(status, keyword);
    }

    private Object listMySubscriptions(JsonNode arguments) {
        SubscriptionStatus status = optionalEnum(arguments, "status", SubscriptionStatus.class);
        List<Subscription> subscriptions = subscriptionPortIn.getSubscriptions(null, null, null, null, status, null, null, null);
        return Map.of("subscriptions", subscriptions);
    }

    private Object createSupportTicket(JsonNode arguments, UUID conversationId, String idempotencyKey) {
        SupportTicket ticket = new SupportTicket();
        ticket.setCategoryId(UUID.fromString(requiredText(arguments, "categoryId")));
        ticket.setTitle(requiredText(arguments, "title"));
        ticket.setContent(requiredText(arguments, "content"));
        return supportTicketPortIn.createTicketFromConversation(ticket, conversationId, idempotencyKey);
    }

    private String requiredText(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.isTextual()) {
            throw new BadRequestException(field + " is required");
        }
        return value.asText().trim();
    }

    private String optionalText(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
    }

    private <E extends Enum<E>> E optionalEnum(JsonNode arguments, String field, Class<E> enumType) {
        String value = optionalText(arguments, field);
        return value == null ? null : Enum.valueOf(enumType, value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize AI tool response");
        }
    }

    public record ToolExecutionResult(String toolName, String responseJson, boolean sensitive) {
    }
}
