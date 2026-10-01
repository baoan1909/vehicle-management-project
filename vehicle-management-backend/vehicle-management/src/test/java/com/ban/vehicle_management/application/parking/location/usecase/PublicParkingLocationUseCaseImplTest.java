package com.ban.vehicle_management.application.parking.location.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.location.model.ParkingLocationSearchResult;
import com.ban.vehicle_management.application.parking.location.port.out.ParkingLocationPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicParkingLocationUseCaseImplTest {

    @Mock
    private ParkingLocationPortOut parkingLocationPortOut;

    @InjectMocks
    private PublicParkingLocationUseCaseImpl useCase;

    @Test
    void shouldNormalizeAndForwardExplicitSearch() {
        ParkingLocationSearchResult result = new ParkingLocationSearchResult(
                "Thủ Đức, Việt Nam",
                new BigDecimal("10.850000"),
                new BigDecimal("106.771000")
        );
        when(parkingLocationPortOut.search("Thủ Đức")).thenReturn(List.of(result));

        assertEquals(List.of(result), useCase.search("  Thủ Đức  "));

        verify(parkingLocationPortOut).search("Thủ Đức");
    }

    @Test
    void shouldRejectShortAndOversizedQueriesBeforeCallingProvider() {
        assertThrows(BadRequestException.class, () -> useCase.search("ab"));
        assertThrows(BadRequestException.class, () -> useCase.search("x".repeat(201)));

        verify(parkingLocationPortOut, never()).search(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectInvalidReverseCoordinatesBeforeCallingProvider() {
        BigDecimal latitude = new BigDecimal("91");
        BigDecimal longitude = new BigDecimal("106.7");

        assertThrows(BadRequestException.class, () -> useCase.reverse(latitude, longitude));

        verify(parkingLocationPortOut, never()).reverse(latitude, longitude);
    }
}
