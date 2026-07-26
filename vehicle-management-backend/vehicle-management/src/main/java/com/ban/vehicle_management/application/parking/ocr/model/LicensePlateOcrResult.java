package com.ban.vehicle_management.application.parking.ocr.model;

import java.util.List;
import java.util.Map;

public record LicensePlateOcrResult(
        String requestId,
        String licensePlate,
        String normalizedLicensePlate,
        String formattedLicensePlate,
        String plateType,
        Boolean validFormat,
        Integer correctionCount,
        LicensePlateOcrBoundingBox bbox,
        Double confidence,
        Double detectorConfidence,
        Double ocrConfidence,
        boolean needsReview,
        Double processingMs,
        String modelVersion,
        String modelStage,
        List<String> reviewReasons,
        List<LicensePlateOcrDetection> detections,
        List<LicensePlateOcrCandidate> candidates,
        Map<String, Object> rawResponse
) {
    public LicensePlateOcrResult withRequestId(String nextRequestId) {
        return new LicensePlateOcrResult(
                nextRequestId,
                licensePlate,
                normalizedLicensePlate,
                formattedLicensePlate,
                plateType,
                validFormat,
                correctionCount,
                bbox,
                confidence,
                detectorConfidence,
                ocrConfidence,
                needsReview,
                processingMs,
                modelVersion,
                modelStage,
                reviewReasons,
                detections,
                candidates,
                rawResponse
        );
    }

    public record LicensePlateOcrCandidate(
            String licensePlate,
            String normalizedLicensePlate,
            String formattedLicensePlate,
            String plateType,
            Boolean validFormat,
            Integer correctionCount,
            LicensePlateOcrBoundingBox bbox,
            Double confidence,
            Double detectorConfidence,
            Double ocrConfidence
    ) {
    }

    public record LicensePlateOcrDetection(
            LicensePlateOcrBoundingBox bbox,
            Double confidence,
            Integer classId
    ) {
    }

    public record LicensePlateOcrBoundingBox(
            Integer x1,
            Integer y1,
            Integer x2,
            Integer y2
    ) {
    }
}