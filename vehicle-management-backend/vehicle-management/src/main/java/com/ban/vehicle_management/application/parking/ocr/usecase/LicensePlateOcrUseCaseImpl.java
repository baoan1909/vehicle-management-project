package com.ban.vehicle_management.application.parking.ocr.usecase;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrCommand;
import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import com.ban.vehicle_management.application.parking.ocr.port.in.LicensePlateOcrPortIn;
import com.ban.vehicle_management.application.parking.ocr.port.out.LicensePlateOcrPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlateFormat;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlatePolicy;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlateResolution;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LicensePlateOcrUseCaseImpl implements LicensePlateOcrPortIn {

    private final LicensePlateOcrPortOut licensePlateOcrPortOut;
    private final ParkingSessionAccessGuard parkingSessionAccessGuard;
    private final LicensePlatePolicy licensePlatePolicy = new LicensePlatePolicy();

    public LicensePlateOcrUseCaseImpl(
            LicensePlateOcrPortOut licensePlateOcrPortOut,
            ParkingSessionAccessGuard parkingSessionAccessGuard
    ) {
        this.licensePlateOcrPortOut = licensePlateOcrPortOut;
        this.parkingSessionAccessGuard = parkingSessionAccessGuard;
    }

    @Override
    public LicensePlateOcrResult recognize(LicensePlateOcrCommand command) {
        parkingSessionAccessGuard.ensureCanUseOcr();
        if (command == null) {
            throw new BadRequestException("OCR command must not be null");
        }
        MultipartFile image = command.image();
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("image must not be empty");
        }

        return applyVehicleManagementPlatePolicy(licensePlateOcrPortOut.recognize(image));
    }

    private LicensePlateOcrResult applyVehicleManagementPlatePolicy(LicensePlateOcrResult ocrResult) {
        String source = hasText(ocrResult.normalizedLicensePlate())
                ? ocrResult.normalizedLicensePlate()
                : ocrResult.licensePlate();
        LicensePlateResolution resolution = resolveSafely(source);
        List<String> reviewReasons = new ArrayList<>(ocrResult.reviewReasons() == null
                ? List.of()
                : ocrResult.reviewReasons());
        if (resolution.needsReview() && !reviewReasons.contains("VM_PLATE_FORMAT_REVIEW")) {
            reviewReasons.add("VM_PLATE_FORMAT_REVIEW");
        }

        List<LicensePlateOcrResult.LicensePlateOcrCandidate> candidates = ocrResult.candidates() == null
                ? List.of()
                : ocrResult.candidates().stream().map(this::normalizeCandidate).toList();

        return new LicensePlateOcrResult(
                ocrResult.requestId(),
                ocrResult.licensePlate(),
                resolution.normalized(),
                resolution.display(),
                resolution.format().name(),
                resolution.validFormat(),
                ocrResult.correctionCount(),
                ocrResult.bbox(),
                ocrResult.confidence(),
                ocrResult.detectorConfidence(),
                ocrResult.ocrConfidence(),
                ocrResult.needsReview() || resolution.needsReview(),
                ocrResult.processingMs(),
                ocrResult.modelVersion(),
                ocrResult.modelStage(),
                List.copyOf(reviewReasons),
                ocrResult.detections(),
                candidates,
                ocrResult.rawResponse()
        );
    }

    private LicensePlateOcrResult.LicensePlateOcrCandidate normalizeCandidate(
            LicensePlateOcrResult.LicensePlateOcrCandidate candidate
    ) {
        String source = hasText(candidate.normalizedLicensePlate())
                ? candidate.normalizedLicensePlate()
                : candidate.licensePlate();
        LicensePlateResolution resolution = resolveSafely(source);
        return new LicensePlateOcrResult.LicensePlateOcrCandidate(
                candidate.licensePlate(),
                resolution.normalized(),
                resolution.display(),
                resolution.format().name(),
                resolution.validFormat(),
                candidate.correctionCount(),
                candidate.bbox(),
                candidate.confidence(),
                candidate.detectorConfidence(),
                candidate.ocrConfidence()
        );
    }

    private LicensePlateResolution resolveSafely(String value) {
        try {
            return licensePlatePolicy.resolve(value, null);
        } catch (BadRequestException ignored) {
            return new LicensePlateResolution(null, null, LicensePlateFormat.UNKNOWN, false, true);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
