package com.ban.vehicle_management.application.parking.ocr.model;

import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public record LicensePlateOcrCommand(
        MultipartFile image,
        UUID laneId,
        LaneDirection direction
) {
}