package com.ban.vehicle_management.entrypoint.policy;

import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class EndpointOutcomePolicy {

    public <T> ResponseEntity<ApiResponse<T>> createdOrOk(
            boolean created, T body, String createdMessage, String existingMessage) {
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.ok(created ? createdMessage : existingMessage, body));
    }

    public <T> ResponseEntity<ApiResponse<T>> acceptedOrOk(
            boolean accepted, T body, String acceptedMessage, String existingMessage) {
        return ResponseEntity.status(accepted ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(ApiResponse.ok(accepted ? acceptedMessage : existingMessage, body));
    }

    public <T> ResponseEntity<ApiResponse<T>> okWithMessage(
            boolean condition, T body, String trueMessage, String falseMessage) {
        return ResponseEntity.ok(ApiResponse.ok(condition ? trueMessage : falseMessage, body));
    }
}
