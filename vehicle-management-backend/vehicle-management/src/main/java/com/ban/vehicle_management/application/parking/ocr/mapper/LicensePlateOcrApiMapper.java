package com.ban.vehicle_management.application.parking.ocr.mapper;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrCommand;
import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import com.ban.vehicle_management.entrypoint.dto.parking.ocr.response.LicensePlateOcrResponse;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import org.springframework.web.multipart.MultipartFile;

@Mapper(componentModel = "spring", nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface LicensePlateOcrApiMapper {

    LicensePlateOcrCommand toCommand(MultipartFile image, UUID laneId, LaneDirection direction);

    LicensePlateOcrResponse toResponse(LicensePlateOcrResult result);

    LicensePlateOcrResponse.LicensePlateOcrBoundingBoxResponse toResponse(
            LicensePlateOcrResult.LicensePlateOcrBoundingBox boundingBox);

    LicensePlateOcrResponse.LicensePlateOcrDetectionResponse toResponse(
            LicensePlateOcrResult.LicensePlateOcrDetection detection);

    LicensePlateOcrResponse.LicensePlateOcrCandidateResponse toResponse(
            LicensePlateOcrResult.LicensePlateOcrCandidate candidate);

    List<LicensePlateOcrResponse.LicensePlateOcrDetectionResponse> toDetectionResponses(
            List<LicensePlateOcrResult.LicensePlateOcrDetection> detections);

    List<LicensePlateOcrResponse.LicensePlateOcrCandidateResponse> toCandidateResponses(
            List<LicensePlateOcrResult.LicensePlateOcrCandidate> candidates);
}
