package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.location.port.in.ParkingLocationFeaturePortIn;
import com.ban.vehicle_management.application.parking.parkinglot.model.result.NearbyParkingLotResult;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.NearbyParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingMapMetricsPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NearbyParkingLotUseCaseImplTest {

    @Mock
    private NearbyParkingLotPortOut nearbyParkingLotPortOut;

    @Mock
    private ParkingMapMetricsPortOut parkingMapMetricsPortOut;

    @Mock
    private ParkingLocationFeaturePortIn featurePortIn;

    private NearbyParkingLotUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new NearbyParkingLotUseCaseImpl(nearbyParkingLotPortOut, parkingMapMetricsPortOut, featurePortIn);
    }

    @Test
    void shouldConvertKilometresToMetersAndDelegateToSpatialPort() {
        BigDecimal latitude = new BigDecimal("10.850123");
        BigDecimal longitude = new BigDecimal("106.771234");
        List<NearbyParkingLotResult> expected = List.of();
        when(nearbyParkingLotPortOut.findNearby(
                latitude,
                longitude,
                new BigDecimal("5000"),
                20
        )).thenReturn(expected);

        List<NearbyParkingLotResult> actual = useCase.findNearby(
                latitude,
                longitude,
                new BigDecimal("5"),
                20
        );

        assertSame(expected, actual);
        verify(nearbyParkingLotPortOut).findNearby(
                latitude,
                longitude,
                new BigDecimal("5000"),
                20
        );
        verify(parkingMapMetricsPortOut).recordNearbySearch(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(true)
        );
    }

    @Test
    void shouldRejectInvalidCoordinates() {
        assertBadRequest(null, decimal("106.7"), decimal("5"), 20);
        assertBadRequest(decimal("10.8"), null, decimal("5"), 20);
        assertBadRequest(decimal("90.000001"), decimal("106.7"), decimal("5"), 20);
        assertBadRequest(decimal("-90.000001"), decimal("106.7"), decimal("5"), 20);
        assertBadRequest(decimal("10.8"), decimal("180.000001"), decimal("5"), 20);
        assertBadRequest(decimal("10.8"), decimal("-180.000001"), decimal("5"), 20);
    }

    @Test
    void shouldRejectInvalidRadius() {
        assertBadRequest(decimal("10.8"), decimal("106.7"), null, 20);
        assertBadRequest(decimal("10.8"), decimal("106.7"), BigDecimal.ZERO, 20);
        assertBadRequest(decimal("10.8"), decimal("106.7"), decimal("-1"), 20);
        assertBadRequest(decimal("10.8"), decimal("106.7"), decimal("5.000001"), 20);
    }

    @Test
    void shouldRejectInvalidLimit() {
        assertBadRequest(decimal("10.8"), decimal("106.7"), decimal("5"), 0);
        assertBadRequest(decimal("10.8"), decimal("106.7"), decimal("5"), 51);
    }

    @Test
    void shouldAcceptBoundaryValues() {
        when(nearbyParkingLotPortOut.findNearby(
                decimal("90"),
                decimal("180"),
                decimal("5000"),
                50
        )).thenReturn(List.of());

        assertEquals(List.of(), useCase.findNearby(
                decimal("90"),
                decimal("180"),
                decimal("5"),
                50
        ));
    }

    private void assertBadRequest(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusKm,
            int limit
    ) {
        assertThrows(
                BadRequestException.class,
                () -> useCase.findNearby(latitude, longitude, radiusKm, limit)
        );
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
