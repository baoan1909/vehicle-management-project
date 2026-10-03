package com.ban.vehicle_management.entrypoint.controller.parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.location.mapper.ParkingLocationApiMapper;
import com.ban.vehicle_management.application.parking.location.model.ParkingLocationSearchResult;
import com.ban.vehicle_management.application.parking.location.port.in.PublicParkingLocationPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.location.response.ParkingLocationResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PublicParkingLocationControllerTest {

    @Mock
    private PublicParkingLocationPortIn publicParkingLocationPortIn;

    @Mock
    private ParkingLocationApiMapper parkingLocationApiMapper;

    @Test
    void shouldReturnOnlyPublicLocationFieldsForSearch() {
        PublicParkingLocationController controller = new PublicParkingLocationController(
                publicParkingLocationPortIn,
                parkingLocationApiMapper
        );
        ParkingLocationSearchResult result = new ParkingLocationSearchResult(
                "Thủ Đức, Việt Nam",
                new BigDecimal("10.850000"),
                new BigDecimal("106.771000")
        );
        ParkingLocationResponse responseDto = new ParkingLocationResponse(
                result.displayName(),
                result.latitude(),
                result.longitude()
        );
        when(publicParkingLocationPortIn.search("Thủ Đức")).thenReturn(List.of(result));
        when(parkingLocationApiMapper.toResponses(List.of(result))).thenReturn(List.of(responseDto));

        ResponseEntity<ApiResponse<List<ParkingLocationResponse>>> response = controller.search("Thủ Đức");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(responseDto), response.getBody().getData());
        verify(publicParkingLocationPortIn).search("Thủ Đức");
    }
}
