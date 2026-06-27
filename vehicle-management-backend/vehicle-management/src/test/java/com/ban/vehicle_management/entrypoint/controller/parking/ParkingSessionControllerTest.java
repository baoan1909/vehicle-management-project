package com.ban.vehicle_management.entrypoint.controller.parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingSessionApiMapper;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckInParkingSessionRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckInResponse;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ParkingSessionControllerTest {

    @Mock
    private ParkingSessionPortIn parkingSessionPortIn;

    @Mock
    private ParkingSessionApiMapper parkingSessionApiMapper;

    @Test
    void shouldParseCheckInRequestJsonPartBeforeCallingUseCase() {
        ParkingSessionController controller = new ParkingSessionController(
                parkingSessionPortIn,
                parkingSessionApiMapper
        );
        UUID laneId = UUID.randomUUID();
        MockMultipartFile licensePlateImage = new MockMultipartFile(
                "licensePlateImage",
                "plate.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );
        MockMultipartFile personImage = new MockMultipartFile(
                "personImage",
                "person.jpg",
                "image/jpeg",
                new byte[] {4, 5, 6}
        );
        CheckInCommand command = new CheckInCommand(
                "RFID-REGISTERED-001",
                laneId,
                "60K8-2301",
                licensePlateImage,
                personImage,
                "Xe dang ky check-in"
        );
        CheckInResult result = new CheckInResult(null, null, null, "SUBSCRIPTION", "OPEN");
        ParkingSessionCheckInResponse responseBody = new ParkingSessionCheckInResponse();
        responseBody.setCustomerType("SUBSCRIPTION");

        when(parkingSessionApiMapper.toCommand(
                any(CheckInParkingSessionRequest.class),
                same(licensePlateImage),
                same(personImage)
        ))
                .thenReturn(command);
        when(parkingSessionPortIn.checkIn(command)).thenReturn(result);
        when(parkingSessionApiMapper.toCheckInResponse(result)).thenReturn(responseBody);

        ResponseEntity<ApiResponse<ParkingSessionCheckInResponse>> response = controller.checkIn(
                """
                {
                  "cardUid": "RFID-REGISTERED-001",
                  "laneId": "%s",
                  "licensePlate": "60K8-2301",
                  "note": "Xe dang ky check-in"
                }
                """.formatted(laneId),
                licensePlateImage,
                personImage
        );

        ArgumentCaptor<CheckInParkingSessionRequest> requestCaptor =
                ArgumentCaptor.forClass(CheckInParkingSessionRequest.class);
        verify(parkingSessionApiMapper).toCommand(requestCaptor.capture(), same(licensePlateImage), same(personImage));
        CheckInParkingSessionRequest parsedRequest = requestCaptor.getValue();
        assertEquals("RFID-REGISTERED-001", parsedRequest.cardUid());
        assertEquals(laneId, parsedRequest.laneId());
        assertEquals("60K8-2301", parsedRequest.licensePlate());
        assertEquals("Xe dang ky check-in", parsedRequest.note());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("SUBSCRIPTION", response.getBody().getData().getCustomerType());
    }

    @Test
    void shouldRejectMalformedCheckInRequestJsonPart() {
        ParkingSessionController controller = new ParkingSessionController(
                parkingSessionPortIn,
                parkingSessionApiMapper
        );
        MockMultipartFile licensePlateImage = new MockMultipartFile(
                "licensePlateImage",
                "plate.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );
        MockMultipartFile personImage = new MockMultipartFile(
                "personImage",
                "person.jpg",
                "image/jpeg",
                new byte[] {4, 5, 6}
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.checkIn("{", licensePlateImage, personImage)
        );

        assertEquals("request part must be valid JSON", exception.getMessage());
    }
}
