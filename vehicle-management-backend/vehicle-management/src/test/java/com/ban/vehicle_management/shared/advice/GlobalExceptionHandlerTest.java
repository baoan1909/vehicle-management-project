package com.ban.vehicle_management.shared.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.TooManyRequestsException;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldReturnConflictStatusForConflictException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/iam/roles");

        ResponseEntity<ApiResponse<Map<String, Object>>> response = globalExceptionHandler.handleConflictException(
                new ConflictException("Role code already exists"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Role code already exists", response.getBody().getMessage());
    }

    @Test
    void shouldReturnBadRequestForMalformedRequestBody() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/catalog/vehicle-types");

        ResponseEntity<ApiResponse<Map<String, Object>>> response = globalExceptionHandler.handleMalformedRequestBody(
                new HttpMessageNotReadableException("Malformed JSON", new MockHttpInputMessage(new byte[0])),
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body", response.getBody().getMessage());
    }

    @Test
    void shouldReturnBadRequestForValidationException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/people/customers");
        BindException bindException = new BindException(new Object(), "request");
        bindException.reject("invalid", "validation failed");

        ResponseEntity<ApiResponse<Map<String, Object>>> response = globalExceptionHandler.handleValidationException(
                bindException,
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request validation failed", response.getBody().getMessage());
    }

    @Test
    void shouldReturnTooManyRequestsForRateLimitException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/public/auth/resend-verification-email");

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                globalExceptionHandler.handleTooManyRequestsException(
                        new TooManyRequestsException("Too many requests"),
                        request
                );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("Too many requests", response.getBody().getMessage());
    }
}
