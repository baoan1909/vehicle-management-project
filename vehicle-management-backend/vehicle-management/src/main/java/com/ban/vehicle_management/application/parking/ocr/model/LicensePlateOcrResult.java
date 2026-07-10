package com.ban.vehicle_management.application.parking.ocr.model;

import java.util.List;

public record LicensePlateOcrResult(
        String licensePlate,
        String normalizedLicensePlate,
        double confidence,
        double detectorConfidence,
        double ocrConfidence,
        boolean needsReview,
        List<LicensePlateOcrCandidate> candidates
) {
    public record LicensePlateOcrCandidate(
            String licensePlate,
            String normalizedLicensePlate,
            double confidence,
            double detectorConfidence,
            double ocrConfidence
    ) {
    }
}
