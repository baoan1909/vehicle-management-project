package com.ban.vehicle_management.application.parking.parkingsession.mapper;

import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckOutCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutPreviewResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlatePolicy;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlateResolution;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingevent.response.ParkingEventResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckInParkingSessionRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckOutParkingSessionRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckInResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckOutResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckOutPreviewResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionManagementResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.springframework.web.multipart.MultipartFile;

@Mapper(componentModel = "spring")
public interface ParkingSessionApiMapper {

    @Mapping(target = "licensePlateImage", ignore = true)
    @Mapping(target = "personImage", ignore = true)
    CheckInCommand toCommand(CheckInParkingSessionRequest request);

    default CheckInCommand toCommand(
            CheckInParkingSessionRequest request,
            MultipartFile licensePlateImage,
            MultipartFile personImage
    ) {
        CheckInCommand command = toCommand(request);
        return new CheckInCommand(
                command.cardUid(),
                command.laneId(),
                command.vehicleTypeId(),
                command.licensePlate(),
                licensePlateImage,
                personImage,
                command.note(),
                command.plateFormatConfirmed(),
                command.plateIdentityOverride()
        );
    }

    ParkingSessionCheckInResponse toCheckInResponse(CheckInResult result);

    @Mapping(target = "licensePlateImage", ignore = true)
    @Mapping(target = "personImage", ignore = true)
    CheckOutCommand toCommand(CheckOutParkingSessionRequest request);

    default CheckOutCommand toCommand(
            CheckOutParkingSessionRequest request,
            MultipartFile licensePlateImage,
            MultipartFile personImage
    ) {
        CheckOutCommand command = toCommand(request);
        return new CheckOutCommand(
                command.laneId(),
                command.cardUid(),
                command.licensePlate(),
                licensePlateImage,
                personImage,
                command.note()
        );
    }

    ParkingSessionCheckOutResponse toCheckOutResponse(CheckOutResult result);

    ParkingSessionCheckOutPreviewResponse toCheckOutPreviewResponse(CheckOutPreviewResult result);

    List<ParkingSessionManagementResponse> toManagementResponses(List<ParkingSessionManagementResult> result);

    @Mapping(target = "licensePlateInNormalized", ignore = true)
    @Mapping(target = "licensePlateInDisplay", ignore = true)
    @Mapping(target = "licensePlateInFormat", ignore = true)
    @Mapping(target = "licensePlateInValidFormat", ignore = true)
    @Mapping(target = "licensePlateInNeedsReview", ignore = true)
    @Mapping(target = "licensePlateOutNormalized", ignore = true)
    @Mapping(target = "licensePlateOutDisplay", ignore = true)
    @Mapping(target = "licensePlateOutFormat", ignore = true)
    @Mapping(target = "licensePlateOutValidFormat", ignore = true)
    @Mapping(target = "licensePlateOutNeedsReview", ignore = true)
    ParkingSessionManagementResponse toManagementResponse(ParkingSessionManagementResult result);

    @Mapping(target = "licensePlateDetectedNormalized", ignore = true)
    @Mapping(target = "licensePlateDetectedDisplay", ignore = true)
    @Mapping(target = "licensePlateFormat", ignore = true)
    @Mapping(target = "validFormat", ignore = true)
    @Mapping(target = "needsReview", ignore = true)
    ParkingSessionManagementResponse.EventResponse toManagementEventResponse(ParkingSessionManagementResult.EventResult result);

    @Mapping(target = "licensePlateInNormalized", ignore = true)
    @Mapping(target = "licensePlateInDisplay", ignore = true)
    @Mapping(target = "licensePlateInFormat", ignore = true)
    @Mapping(target = "licensePlateInValidFormat", ignore = true)
    @Mapping(target = "licensePlateInNeedsReview", ignore = true)
    @Mapping(target = "licensePlateOutNormalized", ignore = true)
    @Mapping(target = "licensePlateOutDisplay", ignore = true)
    @Mapping(target = "licensePlateOutFormat", ignore = true)
    @Mapping(target = "licensePlateOutValidFormat", ignore = true)
    @Mapping(target = "licensePlateOutNeedsReview", ignore = true)
    ParkingSessionResponse toResponse(ParkingSession parkingSession);

    @Mapping(target = "licensePlateDetectedNormalized", ignore = true)
    @Mapping(target = "licensePlateDetectedDisplay", ignore = true)
    @Mapping(target = "licensePlateFormat", ignore = true)
    @Mapping(target = "validFormat", ignore = true)
    @Mapping(target = "needsReview", ignore = true)
    ParkingEventResponse toResponse(ParkingEvent parkingEvent);

    InvoiceAdminResponse toResponse(Invoice invoice);

    @AfterMapping
    default void addPlateView(ParkingSession source, @MappingTarget ParkingSessionResponse target) {
        applySessionPlateView(source.getLicensePlateIn(), source.getLicensePlateOut(), null, target);
    }

    @AfterMapping
    default void addPlateView(ParkingSessionManagementResult source,
                              @MappingTarget ParkingSessionManagementResponse target) {
        LicensePlateResolution plateIn = resolve(source.licensePlateIn(), source.vehicleTypeCode());
        LicensePlateResolution plateOut = resolve(source.licensePlateOut(), source.vehicleTypeCode());
        target.setLicensePlateInNormalized(plateIn.normalized());
        target.setLicensePlateInDisplay(plateIn.display());
        target.setLicensePlateInFormat(plateIn.format().name());
        target.setLicensePlateInValidFormat(plateIn.validFormat());
        target.setLicensePlateInNeedsReview(plateIn.needsReview());
        target.setLicensePlateOutNormalized(plateOut.normalized());
        target.setLicensePlateOutDisplay(plateOut.display());
        target.setLicensePlateOutFormat(plateOut.format().name());
        target.setLicensePlateOutValidFormat(plateOut.validFormat());
        target.setLicensePlateOutNeedsReview(plateOut.needsReview());
    }

    @AfterMapping
    default void addPlateView(ParkingEvent source, @MappingTarget ParkingEventResponse target) {
        LicensePlateResolution resolution = resolve(source.getLicensePlateDetected(), null);
        target.setLicensePlateDetectedNormalized(resolution.normalized());
        target.setLicensePlateDetectedDisplay(resolution.display());
        target.setLicensePlateFormat(resolution.format().name());
        target.setValidFormat(resolution.validFormat());
        target.setNeedsReview(resolution.needsReview());
    }

    @AfterMapping
    default void addPlateView(ParkingSessionManagementResult.EventResult source,
                              @MappingTarget ParkingSessionManagementResponse.EventResponse target) {
        LicensePlateResolution resolution = resolve(source.licensePlateDetected(), null);
        target.setLicensePlateDetectedNormalized(resolution.normalized());
        target.setLicensePlateDetectedDisplay(resolution.display());
        target.setLicensePlateFormat(resolution.format().name());
        target.setValidFormat(resolution.validFormat());
        target.setNeedsReview(resolution.needsReview());
    }

    private void applySessionPlateView(String in, String out, String vehicleTypeCode, ParkingSessionResponse target) {
        LicensePlateResolution plateIn = resolve(in, vehicleTypeCode);
        LicensePlateResolution plateOut = resolve(out, vehicleTypeCode);
        target.setLicensePlateInNormalized(plateIn.normalized());
        target.setLicensePlateInDisplay(plateIn.display());
        target.setLicensePlateInFormat(plateIn.format().name());
        target.setLicensePlateInValidFormat(plateIn.validFormat());
        target.setLicensePlateInNeedsReview(plateIn.needsReview());
        target.setLicensePlateOutNormalized(plateOut.normalized());
        target.setLicensePlateOutDisplay(plateOut.display());
        target.setLicensePlateOutFormat(plateOut.format().name());
        target.setLicensePlateOutValidFormat(plateOut.validFormat());
        target.setLicensePlateOutNeedsReview(plateOut.needsReview());
    }

    private LicensePlateResolution resolve(String value, String vehicleTypeCode) {
        return new LicensePlatePolicy().resolve(value, vehicleTypeCode);
    }

}
