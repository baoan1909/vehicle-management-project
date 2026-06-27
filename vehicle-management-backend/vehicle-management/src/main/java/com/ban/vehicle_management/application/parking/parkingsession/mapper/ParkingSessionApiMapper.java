package com.ban.vehicle_management.application.parking.parkingsession.mapper;

import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingevent.response.ParkingEventResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckInParkingSessionRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckInResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
                command.licensePlate(),
                licensePlateImage,
                personImage,
                command.note()
        );
    }

    ParkingSessionCheckInResponse toCheckInResponse(CheckInResult result);

    ParkingSessionResponse toResponse(ParkingSession parkingSession);

    ParkingEventResponse toResponse(ParkingEvent parkingEvent);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
