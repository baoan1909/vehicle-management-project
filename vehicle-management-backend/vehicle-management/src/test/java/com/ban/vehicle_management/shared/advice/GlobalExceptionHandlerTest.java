package com.ban.vehicle_management.shared.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.FeatureDisabledException;
import com.ban.vehicle_management.shared.exception.MapRequestLimitException;
import com.ban.vehicle_management.shared.exception.TooManyRequestsException;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldReturnStructuredServiceUnavailableForDisabledFeature() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/public/parking-lots/nearby");

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                globalExceptionHandler.handleFeatureDisabledException(
                        new FeatureDisabledException("PUBLIC_NEARBY_SEARCH_ENABLED"),
                        request
                );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("FEATURE_DISABLED", response.getBody().getData().get("code"));
        assertEquals("PUBLIC_NEARBY_SEARCH_ENABLED", response.getBody().getData().get("feature"));
    }
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
        assertEquals("Nội dung yêu cầu không đúng định dạng", response.getBody().getMessage());
    }

    @Test
    void shouldReturnUnsupportedMediaTypeForUnsupportedContentType() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/parking/parking-sessions/check-in");

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                globalExceptionHandler.handleMediaTypeNotSupportedException(
                        new HttpMediaTypeNotSupportedException(
                                MediaType.APPLICATION_OCTET_STREAM,
                                List.of(MediaType.MULTIPART_FORM_DATA)
                        ),
                        request
                );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals(415, response.getBody().getData().get("status"));
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
        assertEquals("Dữ liệu gửi lên không hợp lệ", response.getBody().getMessage());
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

    @Test
    void shouldReturnStructuredMapQuotaResponseAndRetryAfterHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/public/parking-locations/search");
        OffsetDateTime retryAfter = OffsetDateTime.now(ZoneOffset.ofHours(7)).plusHours(2);

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                globalExceptionHandler.handleMapRequestLimitException(
                        new MapRequestLimitException(
                                "MAP_DAILY_QUOTA_EXHAUSTED",
                                "Đã hết lượt tra cứu bản đồ hôm nay.",
                                retryAfter
                        ),
                        request
                );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("MAP_DAILY_QUOTA_EXHAUSTED", response.getBody().getData().get("code"));
        assertEquals(retryAfter.toString(), response.getBody().getData().get("retryAfter"));
        assertFalse(response.getHeaders().getFirst("Retry-After").isBlank());
    }

    @Test
    void shouldReturnBadRequestForRejectedRequestParameterPayload() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/people/customers");

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                globalExceptionHandler.handleRequestRejectedException(
                        new RequestRejectedException("The request was rejected because the parameter name \"{...}\" is not allowed."),
                        request
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "Yêu cầu bị từ chối do tham số không đúng định dạng. Vui lòng gửi dữ liệu JSON trong nội dung yêu cầu",
                response.getBody().getMessage()
        );
    }
}
