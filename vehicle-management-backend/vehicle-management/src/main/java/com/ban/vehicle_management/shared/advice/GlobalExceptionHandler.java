package com.ban.vehicle_management.shared.advice;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.exception.TooManyRequestsException;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotFoundException(
            NotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBadRequestException(
            BadRequestException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConflictException(
            ConflictException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleTooManyRequestsException(
            TooManyRequestsException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            BindException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Request validation failed", request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMalformedRequestBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAuthenticationCredentialsNotFoundException(
            AuthenticationCredentialsNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RequestRejectedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRequestRejectedException(
            RequestRejectedException exception,
            HttpServletRequest request
    ) {
        LOGGER.warn("Rejected request at {}: {}", request.getRequestURI(), exception.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                buildRequestRejectedMessage(exception),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Unhandled exception at {}: {}", request.getRequestURI(), exception.getMessage(), exception);
        String debugMessage = "Internal server error: " + exception.getClass().getSimpleName()
                + (exception.getMessage() == null || exception.getMessage().isBlank()
                ? ""
                : " - " + exception.getMessage());
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, debugMessage, request.getRequestURI());
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> buildErrorResponse(
            HttpStatus httpStatus,
            String message,
            String path
    ) {
        ApiResponse<Map<String, Object>> response = ApiResponse.fail(
                message,
                Map.of(
                        "status", httpStatus.value(),
                        "error", httpStatus.getReasonPhrase(),
                        "path", path
                )
        );
        return ResponseEntity.status(httpStatus).body(response);
    }

    private String buildRequestRejectedMessage(RequestRejectedException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("parameter name")) {
            return "Request was rejected before permission checking. Send JSON in the request body with Content-Type: application/json instead of query or form parameters.";
        }
        return "Request was rejected by security validation";
    }
}


