package com.ban.vehicle_management.entrypoint.dto.parking.ocr.response;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import java.util.List;

public record LicensePlateOcrResponse(
        String licensePlate,
        String normalizedLicensePlate,
        double confidence,
        double detectorConfidence,
        double ocrConfidence,
        boolean needsReview,
        List<LicensePlateOcrCandidateResponse> candidates
) {
    public static LicensePlateOcrResponse from(LicensePlateOcrResult result) {
        return new LicensePlateOcrResponse(
                result.licensePlate(),
                result.normalizedLicensePlate(),
                result.confidence(),
                result.detectorConfidence(),
                result.ocrConfidence(),
                result.needsReview(),
                result.candidates() == null
                        ? List.of()
                        : result.candidates().stream()
                        .map(LicensePlateOcrCandidateResponse::from)
                        .toList()
        );
    }

    public record LicensePlateOcrCandidateResponse(
            String licensePlate,
            String normalizedLicensePlate,
            double confidence,
            double detectorConfidence,
            double ocrConfidence
    ) {
        public static LicensePlateOcrCandidateResponse from(LicensePlateOcrResult.LicensePlateOcrCandidate candidate) {
            return new LicensePlateOcrCandidateResponse(
                    candidate.licensePlate(),
                    candidate.normalizedLicensePlate(),
                    candidate.confidence(),
                    candidate.detectorConfidence(),
                    candidate.ocrConfidence()
            );
        }
    }
}
