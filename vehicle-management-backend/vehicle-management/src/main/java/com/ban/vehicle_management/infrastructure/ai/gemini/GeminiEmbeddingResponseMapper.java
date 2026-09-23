package com.ban.vehicle_management.infrastructure.ai.gemini;

import com.ban.vehicle_management.domain.ai.model.EmbeddingFailure;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class GeminiEmbeddingResponseMapper {

    private final ObjectMapper objectMapper;

    public GeminiEmbeddingResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EmbeddingResult toEmbeddingResult(int statusCode, String body, Long retryAfterSeconds, int expectedDimension) {
        if (statusCode < 200 || statusCode >= 300) {
            return toFailureResult(statusCode, body, retryAfterSeconds);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode values = root.path("embedding").path("values");
            if (!values.isArray() || values.isEmpty()) {
                return failure("EMBEDDING_INVALID_RESPONSE", statusCode, null, null, true, retryAfterSeconds);
            }
            double[] vector = new double[values.size()];
            int index = 0;
            for (JsonNode value : values) {
                double component = value.isNumber() ? value.asDouble() : Double.NaN;
                if (Double.isNaN(component) || Double.isInfinite(component)) {
                    return failure("EMBEDDING_VECTOR_INVALID", statusCode, null, null, false, retryAfterSeconds);
                }
                vector[index++] = component;
            }
            if (vector.length != expectedDimension) {
                return failure("EMBEDDING_DIMENSION_MISMATCH", statusCode, null, null, false, retryAfterSeconds);
            }
            return EmbeddingResult.success(EmbeddingVector.of(vector, expectedDimension), null);
        } catch (Exception exception) {
            return failure("EMBEDDING_INVALID_RESPONSE", statusCode, null, null, true, retryAfterSeconds);
        }
    }

    private EmbeddingResult toFailureResult(int statusCode, String body, Long retryAfterSeconds) {
        boolean retryable = isRetryableStatus(statusCode);
        String code = switch (statusCode) {
            case 400 -> "EMBEDDING_INVALID_REQUEST";
            case 401 -> "EMBEDDING_AUTHENTICATION_FAILED";
            case 403 -> "EMBEDDING_PERMISSION_DENIED";
            case 404 -> "EMBEDDING_MODEL_NOT_FOUND";
            case 408 -> "EMBEDDING_TIMEOUT";
            case 429 -> "EMBEDDING_RATE_LIMITED";
            default -> statusCode >= 500 ? "EMBEDDING_PROVIDER_UNAVAILABLE" : "EMBEDDING_INVALID_REQUEST";
        };
        return failure(code, statusCode, providerErrorCode(body), providerErrorMessage(body), retryable, retryAfterSeconds);
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private EmbeddingResult failure(
            String code,
            int statusCode,
            String providerErrorCode,
            String providerErrorMessage,
            boolean retryable,
            Long retryAfterSeconds
    ) {
        return EmbeddingResult.failure(new EmbeddingFailure(
                code,
                statusCode,
                providerErrorCode,
                providerErrorMessage,
                retryable,
                retryAfterSeconds
        ));
    }

    private String providerErrorCode(String body) {
        return sanitize(textOrNull(errorNode(body).path("status")), 80);
    }

    private String providerErrorMessage(String body) {
        return sanitize(textOrNull(errorNode(body).path("message")), 500);
    }

    private JsonNode errorNode(String body) {
        try {
            return objectMapper.readTree(body == null || body.isBlank() ? "{}" : body).path("error");
        } catch (Exception exception) {
            return objectMapper.getNodeFactory().objectNode();
        }
    }

    private String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value
                .replaceAll("(?i)(x-goog-api-key|api[-_ ]?key|authorization|bearer|token)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]")
                .replace('<', '[')
                .replace('>', ']')
                .trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}