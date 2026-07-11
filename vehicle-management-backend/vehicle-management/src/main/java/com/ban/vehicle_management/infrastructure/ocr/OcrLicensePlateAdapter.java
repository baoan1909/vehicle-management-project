package com.ban.vehicle_management.infrastructure.ocr;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult.LicensePlateOcrCandidate;
import com.ban.vehicle_management.application.parking.ocr.port.out.LicensePlateOcrPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
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
            OcrServiceResponse response = restClient.post()
                    .uri("/v1/license-plate/recognize")
                    .header(OCR_TOKEN_HEADER, properties.getInternalToken())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(toMultipartBody(image))
                    .retrieve()
                    .body(OcrServiceResponse.class);

            if (response == null) {
                throw new ConflictException("OCR service returned an empty response");
            }

            return response.toResult(properties.getConfidenceThreshold());
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OcrErrorResponse(String detail) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OcrServiceResponse(
            String license_plate,
            String normalized_license_plate,
            double confidence,
            double detector_confidence,
            double ocr_confidence,
            boolean needs_review,
            List<OcrCandidateResponse> candidates
    ) {
        LicensePlateOcrResult toResult(double confidenceThreshold) {
            boolean reviewRequired = needs_review || confidence < confidenceThreshold;
            List<LicensePlateOcrCandidate> mappedCandidates = candidates == null
                    ? List.of()
                    : candidates.stream()
                    .map(OcrCandidateResponse::toCandidate)
                    .toList();

            return new LicensePlateOcrResult(
                    license_plate,
                    normalized_license_plate,
                    confidence,
                    detector_confidence,
                    ocr_confidence,
                    reviewRequired,
                    mappedCandidates
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OcrCandidateResponse(
            String license_plate,
            String normalized_license_plate,
            double confidence,
            double detector_confidence,
            double ocr_confidence
    ) {
        LicensePlateOcrCandidate toCandidate() {
            return new LicensePlateOcrCandidate(
                    license_plate,
                    normalized_license_plate,
                    confidence,
                    detector_confidence,
                    ocr_confidence
            );
        }
    }
}
