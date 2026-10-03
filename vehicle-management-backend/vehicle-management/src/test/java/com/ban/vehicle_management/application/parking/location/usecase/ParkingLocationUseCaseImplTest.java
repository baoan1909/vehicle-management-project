package com.ban.vehicle_management.application.parking.location.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.location.model.ParkingLocationSearchResult;
import com.ban.vehicle_management.application.parking.location.port.in.ParkingLocationFeaturePortIn;
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
class ParkingLocationUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private ParkingLocationPortOut parkingLocationPortOut;

    @Mock
    private ParkingLocationFeaturePortIn featurePortIn;

    @InjectMocks
    private ParkingLocationUseCaseImpl useCase;

    @Test
    void shouldAllowUpdateAdministratorToSearchOnlyOnExplicitCall() {
        when(currentAccountPortIn.hasPermission("PARKING_LOT_CREATE_ALL")).thenReturn(false);
        when(currentAccountPortIn.hasPermission("PARKING_LOT_UPDATE_ALL")).thenReturn(true);
        ParkingLocationSearchResult result = new ParkingLocationSearchResult(
                "Thủ Đức, Việt Nam",
                new BigDecimal("10.850000"),
                new BigDecimal("106.771000")
        );
        when(parkingLocationPortOut.search("Thủ Đức")).thenReturn(List.of(result));

        assertEquals(List.of(result), useCase.search("  Thủ Đức  "));

        verify(parkingLocationPortOut).search("Thủ Đức");
        verify(currentAccountPortIn, never()).requirePermission("PARKING_LOT_CREATE_ALL");
    }

    @Test
    void shouldRejectInvalidReverseCoordinatesBeforeCallingProvider() {
        when(currentAccountPortIn.hasPermission("PARKING_LOT_CREATE_ALL")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> useCase.reverse(
                new BigDecimal("91"),
                new BigDecimal("106.7")
        ));

        verify(parkingLocationPortOut, never()).reverse(
                new BigDecimal("91"),
                new BigDecimal("106.7")
        );
    }
}
