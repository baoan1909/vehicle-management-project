package com.ban.vehicle_management.entrypoint.dto.parking.ocr.response;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import java.util.List;
import java.util.Map;

public record LicensePlateOcrResponse(
        String requestId,
        String licensePlate,
        String normalizedLicensePlate,
        String formattedLicensePlate,
        String plateType,
        Boolean validFormat,
        Integer correctionCount,
        LicensePlateOcrBoundingBoxResponse bbox,
        Double confidence,
        Double detectorConfidence,
        Double ocrConfidence,
        boolean needsReview,
        Double processingMs,
        String modelVersion,
        String modelStage,
        List<String> reviewReasons,
        List<LicensePlateOcrDetectionResponse> detections,
        List<LicensePlateOcrCandidateResponse> candidates,
        Map<String, Object> rawResponse
) {
    public static LicensePlateOcrResponse from(LicensePlateOcrResult result) {
        return new LicensePlateOcrResponse(
                result.requestId(),
                result.licensePlate(),
                result.normalizedLicensePlate(),
                result.formattedLicensePlate(),
                result.plateType(),
                result.validFormat(),
                result.correctionCount(),
                LicensePlateOcrBoundingBoxResponse.from(result.bbox()),
                result.confidence(),
                result.detectorConfidence(),
                result.ocrConfidence(),
                result.needsReview(),
                result.processingMs(),
                result.modelVersion(),
                result.modelStage(),
                result.reviewReasons() == null ? List.of() : result.reviewReasons(),
                result.detections() == null
                        ? List.of()
                        : result.detections().stream()
                        .map(LicensePlateOcrDetectionResponse::from)
                        .toList(),
                result.candidates() == null
                        ? List.of()
                        : result.candidates().stream()
                        .map(LicensePlateOcrCandidateResponse::from)
                        .toList(),
                result.rawResponse() == null ? Map.of() : result.rawResponse()
        );
    }

    public record LicensePlateOcrCandidateResponse(
            String licensePlate,
            String normalizedLicensePlate,
            String formattedLicensePlate,
            String plateType,
            Boolean validFormat,
            Integer correctionCount,
            LicensePlateOcrBoundingBoxResponse bbox,
            Double confidence,
            Double detectorConfidence,
            Double ocrConfidence
    ) {
        public static LicensePlateOcrCandidateResponse from(LicensePlateOcrResult.LicensePlateOcrCandidate candidate) {
            return new LicensePlateOcrCandidateResponse(
                    candidate.licensePlate(),
                    candidate.normalizedLicensePlate(),
                    candidate.formattedLicensePlate(),
                    candidate.plateType(),
                    candidate.validFormat(),
                    candidate.correctionCount(),
                    LicensePlateOcrBoundingBoxResponse.from(candidate.bbox()),
                    candidate.confidence(),
                    candidate.detectorConfidence(),
                    candidate.ocrConfidence()
            );
        }
    }

    public record LicensePlateOcrDetectionResponse(
            LicensePlateOcrBoundingBoxResponse bbox,
            Double confidence,
            Integer classId
    ) {
        public static LicensePlateOcrDetectionResponse from(LicensePlateOcrResult.LicensePlateOcrDetection detection) {
            return new LicensePlateOcrDetectionResponse(
                    LicensePlateOcrBoundingBoxResponse.from(detection.bbox()),
                    detection.confidence(),
                    detection.classId()
            );
        }
    }

    public record LicensePlateOcrBoundingBoxResponse(
            Integer x1,
            Integer y1,
            Integer x2,
            Integer y2
    ) {
        public static LicensePlateOcrBoundingBoxResponse from(LicensePlateOcrResult.LicensePlateOcrBoundingBox bbox) {
            if (bbox == null) {
                return null;
            }
            return new LicensePlateOcrBoundingBoxResponse(
                    bbox.x1(),
                    bbox.y1(),
                    bbox.x2(),
                    bbox.y2()
            );
        }
    }
}