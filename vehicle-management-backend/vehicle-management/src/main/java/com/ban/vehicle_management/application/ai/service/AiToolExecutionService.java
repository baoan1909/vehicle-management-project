package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalContext;
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
    private final com.ban.vehicle_management.application.people.customervehicle.port.in.CustomerVehiclePortIn customerVehiclePortIn;
    private final com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn parkingSessionPortIn;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final KnowledgeAccessContextResolver accessContextResolver;
    private final ObjectMapper objectMapper;

    public AiToolExecutionService(
            AiToolRegistry toolRegistry,
            CurrentAccountPortIn currentAccountPortIn,
            SupportTicketPortIn supportTicketPortIn,
            SubscriptionPortIn subscriptionPortIn,
            com.ban.vehicle_management.application.people.customervehicle.port.in.CustomerVehiclePortIn customerVehiclePortIn,
            com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn parkingSessionPortIn,
            KnowledgeRetrievalService knowledgeRetrievalService,
            KnowledgeAccessContextResolver accessContextResolver,
            ObjectMapper objectMapper
    ) {
        this.toolRegistry = toolRegistry;
        this.currentAccountPortIn = currentAccountPortIn;
        this.supportTicketPortIn = supportTicketPortIn;
        this.subscriptionPortIn = subscriptionPortIn;
        this.customerVehiclePortIn = customerVehiclePortIn;
        this.parkingSessionPortIn = parkingSessionPortIn;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.accessContextResolver = accessContextResolver;
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
            case "list_my_vehicles" -> Map.of("responseText", AssistantOwnDataFormatter.vehicles(customerVehiclePortIn.getMyCustomerVehicles(20)));
            case "list_my_parking_history" -> listMyParkingHistory(arguments);
            case "create_support_ticket" -> createSupportTicket(arguments, conversationId, idempotencyKey);
            default -> throw new BadRequestException("AI tool is not executable");
        };
        return new ToolExecutionResult(
                definition.name(), toJson(result), !"search_support_knowledge".equals(definition.name()));
    }

    public ToolExecutionResult executeFromAssistantWorker(String toolName, JsonNode arguments) {
        AiToolDefinition definition = validateToolCall(toolName, arguments);
        Object result = switch (definition.name()) {
            case "search_support_knowledge" -> searchKnowledge(arguments);
            case "get_my_support_ticket" -> getMySupportTicket(arguments);
            case "list_my_support_tickets" -> listMySupportTickets(arguments);
            case "get_my_subscription_status" -> listMySubscriptions(arguments);
            case "list_my_vehicles" -> Map.of("responseText", AssistantOwnDataFormatter.vehicles(customerVehiclePortIn.getMyCustomerVehicles(20)));
            case "list_my_parking_history" -> listMyParkingHistory(arguments);
            default -> throw new BadRequestException("AI tool requires user confirmation or request-time authorization");
        };
        return new ToolExecutionResult(
                definition.name(), toJson(result), !"search_support_knowledge".equals(definition.name()));
    }

    private Object searchKnowledge(JsonNode arguments) {
        String query = requiredText(arguments, "query");
        KnowledgeRetrievalResult result = knowledgeRetrievalService.search(
                KnowledgeRetrievalContext.global(),
                query,
                accessContextResolver.resolveScopes(),
                5
        );
        return Map.of(
                "responseText",
                result.hasResults()
                        ? "Đã tìm thấy " + result.results().size() + " tài liệu hỗ trợ phù hợp."
                        : "Chưa có tài liệu hỗ trợ đủ để trả lời. Bạn có thể tạo phiếu hỗ trợ để nhân viên xử lý.",
                "citations",
                result.results(),
                "diagnosticCode",
                result.diagnosticCode()
        );
    }

    private Object getMySupportTicket(JsonNode arguments) {
        SupportTicket ticket = supportTicketPortIn.getTicketById(UUID.fromString(requiredText(arguments, "supportTicketId")));
        if (!currentAccountPortIn.getCurrentAccountIdOrThrow().equals(ticket.getRequesterAccountId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied");
        }
        return Map.of("responseText", AssistantOwnDataFormatter.supportTickets(List.of(ticket)));
    }

    private Object listMySupportTickets(JsonNode arguments) {
        SupportTicketStatus status = optionalEnum(arguments, "status", SupportTicketStatus.class);
        String keyword = optionalText(arguments, "keyword");
        return Map.of("responseText", AssistantOwnDataFormatter.supportTickets(supportTicketPortIn.getMyTickets(status, keyword, 20)));
    }

    private Object listMySubscriptions(JsonNode arguments) {
        SubscriptionStatus status = optionalEnum(arguments, "status", SubscriptionStatus.class);
        List<Subscription> subscriptions = subscriptionPortIn.getMySubscriptions(20).stream()
                .filter(row -> status == null || row.getStatus() == status).toList();
        return Map.of("responseText", AssistantOwnDataFormatter.subscriptions(subscriptions));
    }

    private Object listMyParkingHistory(JsonNode arguments) {
        var status = optionalEnum(arguments, "status", com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus.class);
        var rows = parkingSessionPortIn.getOwnSessionSummaries(status, 20);
        return Map.of("responseText", AssistantOwnDataFormatter.parkingHistory(rows));
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
