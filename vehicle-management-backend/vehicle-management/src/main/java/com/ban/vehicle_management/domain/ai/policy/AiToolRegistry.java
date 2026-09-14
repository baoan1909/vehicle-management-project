package com.ban.vehicle_management.domain.ai.policy;

import com.ban.vehicle_management.shared.enumeration.ai.AiToolType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AiToolRegistry {

    private final Map<String, AiToolDefinition> tools;

    public AiToolRegistry() {
        List<AiToolDefinition> definitions = List.of(
                readOnly("search_support_knowledge", "Search approved CoParking support documents. Use only for static guidance and policies.",
                        schema("""
                                {"query":{"type":"string","description":"Short support question to search for"}}
                                """, "query"), "SUPPORT_WIDGET_ACCESS_OWN", this::validateSearchKnowledge),
                readOnly("get_my_support_ticket", "Get a current user's support ticket by id.",
                        schema("""
                                {"supportTicketId":{"type":"string","format":"uuid"}}
                                """, "supportTicketId"), "SUPPORT_TICKET_READ_OWN", this::validateTicketId),
                readOnly("list_my_support_tickets", "List current user's support tickets.",
                        schema("""
                                {"status":{"type":"string","enum":["OPEN","IN_PROGRESS","RESOLVED","CLOSED"]},"keyword":{"type":"string"}}
                                """), "SUPPORT_TICKET_READ_OWN", this::validateOptionalStatus),
                readOnly("get_my_subscription_status", "List current user's subscription status.",
                        schema("""
                                {"status":{"type":"string","enum":["PENDING","PENDING_PAYMENT","ACTIVE","EXPIRED","CANCELLED","REJECTED"]}}
                                """), "SUBSCRIPTION_READ_OWN", this::validateOptionalSubscriptionStatus),
                write("create_support_ticket", "Create a support ticket from the current assistant conversation after user confirmation.",
                        schema("""
                                {"categoryId":{"type":"string","format":"uuid"},"title":{"type":"string"},"content":{"type":"string"}}
                                """, "categoryId", "title", "content"), "SUPPORT_TICKET_CREATE_OWN", this::validateCreateTicket)
        );
        this.tools = definitions.stream().collect(Collectors.toUnmodifiableMap(AiToolDefinition::name, definition -> definition));
    }

    public List<AiToolDefinition> all() {
        return List.copyOf(tools.values());
    }

    public Optional<AiToolDefinition> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public AiToolDefinition require(String name) {
        return find(name).orElseThrow(() -> new BadRequestException("AI tool is not allowed"));
    }

    private AiToolDefinition readOnly(String name, String description, String schema, String permission, java.util.function.Consumer<JsonNode> validator) {
        return new AiToolDefinition(name, description, schema, AiToolType.READ_ONLY, permission, false, Duration.ofSeconds(8), "RUN", validator);
    }

    private AiToolDefinition write(String name, String description, String schema, String permission, java.util.function.Consumer<JsonNode> validator) {
        return new AiToolDefinition(name, description, schema, AiToolType.WRITE, permission, true, Duration.ofMinutes(10), "USER_CONFIRMATION", validator);
    }

    private String schema(String properties, String... requiredFields) {
        String required = requiredFields.length == 0
                ? ""
                : ",\"required\":[%s]".formatted(java.util.Arrays.stream(requiredFields)
                        .map(field -> "\"" + field + "\"")
                        .collect(Collectors.joining(",")));
        return """
                {"type":"object","properties":%s,"additionalProperties":false%s}
                """.formatted(properties.trim(), required);
    }

    private void validateSearchKnowledge(JsonNode arguments) {
        requireText(arguments, "query", 3, 300);
    }

    private void validateTicketId(JsonNode arguments) {
        requireText(arguments, "supportTicketId", 10, 60);
    }

    private void validateOptionalStatus(JsonNode arguments) {
        optionalText(arguments, "status", 2, 30);
        optionalText(arguments, "keyword", 1, 120);
    }

    private void validateOptionalSubscriptionStatus(JsonNode arguments) {
        optionalText(arguments, "status", 2, 30);
    }

    private void validateCreateTicket(JsonNode arguments) {
        requireText(arguments, "categoryId", 10, 60);
        requireText(arguments, "title", 3, 150);
        requireText(arguments, "content", 5, 4000);
    }

    private void requireText(JsonNode arguments, String field, int minLength, int maxLength) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.isTextual()) {
            throw new BadRequestException(field + " is required");
        }
        validateText(field, value.asText(), minLength, maxLength);
    }

    private void optionalText(JsonNode arguments, String field, int minLength, int maxLength) {
        JsonNode value = arguments.get(field);
        if (value != null && !value.isNull()) {
            if (!value.isTextual()) {
                throw new BadRequestException(field + " must be a string");
            }
            validateText(field, value.asText(), minLength, maxLength);
        }
    }

    private void validateText(String field, String value, int minLength, int maxLength) {
        String text = value == null ? "" : value.trim();
        if (text.length() < minLength || text.length() > maxLength || text.indexOf('<') >= 0 || text.indexOf('>') >= 0) {
            throw new BadRequestException(field + " is invalid");
        }
    }
}
