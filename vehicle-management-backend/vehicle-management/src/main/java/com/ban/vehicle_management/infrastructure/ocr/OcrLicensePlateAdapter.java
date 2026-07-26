package com.ban.vehicle_management.infrastructure.ocr;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult.LicensePlateOcrBoundingBox;
import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult.LicensePlateOcrCandidate;
import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult.LicensePlateOcrDetection;
import com.ban.vehicle_management.application.parking.ocr.port.out.LicensePlateOcrPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OcrLicensePlateAdapter implements LicensePlateOcrPortOut {

    private static final String OCR_TOKEN_HEADER = "X-Internal-Token";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECTS = new TypeReference<>() {
    };

    private final OcrProperties properties;
    private final RestClient restClient;

    public OcrLicensePlateAdapter(OcrProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public LicensePlateOcrResult recognize(MultipartFile image) {
        if (!properties.isEnabled()) {
            throw new ConflictException("OCR service is disabled");
        }
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("image must not be empty");
        }

        try {
            String responseBody = restClient.post()
                    .uri("/v1/license-plate/recognize")
                    .header(OCR_TOKEN_HEADER, properties.getInternalToken())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(toMultipartBody(image))
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(responseBody)) {
                throw new ConflictException("OCR service returned an empty response");
            }

            Map<String, Object> rawResponse = OBJECT_MAPPER.readValue(responseBody, MAP_OF_OBJECTS);
            OcrServiceResponse response = OBJECT_MAPPER.convertValue(rawResponse, OcrServiceResponse.class);
            return response.toResult(properties.getConfidenceThreshold(), rawResponse);
        } catch (IOException exception) {
            throw new BadRequestException("Could not read OCR image");
        } catch (RestClientResponseException exception) {
            String message = extractOcrErrorMessage(exception);
            if (exception.getStatusCode().is4xxClientError()) {
                throw new BadRequestException(message);
            }
            throw new ConflictException(message);
        } catch (RestClientException exception) {
            throw new ConflictException("Could not recognize license plate. Please enter it manually.");
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("OCR service returned an unsupported response");
        }
    }

    private MultiValueMap<String, Object> toMultipartBody(MultipartFile image) throws IOException {
        String filename = StringUtils.hasText(image.getOriginalFilename())
                ? image.getOriginalFilename()
                : "license-plate.jpg";

        ByteArrayResource imageResource = new ByteArrayResource(image.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imageResource);
        return body;
    }

    private static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "http://localhost:8010";
        }
        return value.replaceAll("/+$", "");
    }

    private static String extractOcrErrorMessage(RestClientResponseException exception) {
        String fallback = "Could not recognize license plate. Please enter it manually.";
        String body = exception.getResponseBodyAsString();
        if (!StringUtils.hasText(body)) {
            return fallback;
        }

        try {
            OcrErrorResponse response = OBJECT_MAPPER.readValue(body, OcrErrorResponse.class);
            if (StringUtils.hasText(response.detail())) {
                return "OCR service error: " + response.detail();
            }
        } catch (IOException ignored) {
            return "OCR service error: " + body;
        }

        return fallback;
    }

    private static double numberOrZero(Double value) {
        return value == null || !Double.isFinite(value) ? 0.0 : value;
    }

    private static Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static LicensePlateOcrBoundingBox toBoundingBox(Map<String, Object> bbox) {
        if (bbox == null || bbox.isEmpty()) {
            return null;
        }
        return new LicensePlateOcrBoundingBox(
                integerValue(bbox.get("x1")),
                integerValue(bbox.get("y1")),
                integerValue(bbox.get("x2")),
                integerValue(bbox.get("y2"))
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OcrErrorResponse(String detail) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OcrServiceResponse(
            String request_id,
            String license_plate,
            String normalized_license_plate,
            String formatted_license_plate,
            String plate_type,
            Boolean valid_format,
            Integer correction_count,
            Map<String, Object> bbox,
            Double confidence,
            Double detector_confidence,
            Double ocr_confidence,
            Boolean needs_review,
            Double processing_ms,
            String model_version,
            String model_stage,
            List<String> review_reasons,
            List<OcrDetectionResponse> detections,
            List<OcrCandidateResponse> candidates
    ) {
        LicensePlateOcrResult toResult(double confidenceThreshold, Map<String, Object> rawResponse) {
            double confidenceValue = numberOrZero(confidence);
            boolean reviewRequired = Boolean.TRUE.equals(needs_review) || confidenceValue < confidenceThreshold;
            List<String> mappedReviewReasons = new ArrayList<>(review_reasons == null ? List.of() : review_reasons);
            if (confidenceValue < confidenceThreshold && !mappedReviewReasons.contains("below_backend_confidence_threshold")) {
                mappedReviewReasons.add("below_backend_confidence_threshold");
            }
            List<LicensePlateOcrCandidate> mappedCandidates = candidates == null
                    ? List.of()
                    : candidates.stream()
                    .map(OcrCandidateResponse::toCandidate)
                    .toList();
            List<LicensePlateOcrDetection> mappedDetections = detections == null
                    ? List.of()
                    : detections.stream()
                    .map(OcrDetectionResponse::toDetection)
                    .toList();

            return new LicensePlateOcrResult(
                    request_id,
                    license_plate,
                    normalized_license_plate,
                    formatted_license_plate,
                    plate_type,
                    valid_format,
                    correction_count,
                    toBoundingBox(bbox),
                    confidence,
                    detector_confidence,
                    ocr_confidence,
                    reviewRequired,
                    processing_ms,
                    model_version,
                    model_stage,
                    mappedReviewReasons,
                    mappedDetections,
                    mappedCandidates,
                    rawResponse
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OcrCandidateResponse(
            String license_plate,
            String normalized_license_plate,
            String formatted_license_plate,
            String plate_type,
            Boolean valid_format,
            Integer correction_count,
            Map<String, Object> bbox,
            Double confidence,
            Double detector_confidence,
            Double ocr_confidence
    ) {
        LicensePlateOcrCandidate toCandidate() {
            return new LicensePlateOcrCandidate(
                    license_plate,
                    normalized_license_plate,
                    formatted_license_plate,
                    plate_type,
                    valid_format,
                    correction_count,
                    toBoundingBox(bbox),
                    confidence,
                    detector_confidence,
                    ocr_confidence
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OcrDetectionResponse(
            Map<String, Object> bbox,
            Double confidence,
            Integer class_id
    ) {
        LicensePlateOcrDetection toDetection() {
            return new LicensePlateOcrDetection(
                    toBoundingBox(bbox),
                    confidence,
                    class_id
            );
        }
    }
}