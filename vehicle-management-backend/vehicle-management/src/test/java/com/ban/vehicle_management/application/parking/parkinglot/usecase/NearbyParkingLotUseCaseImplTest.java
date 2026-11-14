package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.location.port.in.ParkingLocationFeaturePortIn;
import com.ban.vehicle_management.application.parking.parkinglot.model.result.NearbyParkingLotResult;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.NearbyParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingMapMetricsPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import org.mockito.InjectMocks;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @InjectMocks
    private NearbyParkingLotUseCaseImpl useCase;

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
    void shouldRejectMissingRadius() {
        assertBadRequest(decimal("10.8"), decimal("106.7"), null, 20);
        verifyNoInteractions(nearbyParkingLotPortOut, parkingMapMetricsPortOut);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "-1", "0", "0.01", "0.5", "0.999999", "1.000001", "2.5", "12.75",
            "49.999", "50.000001", "51", "999", "1E-10", "1E+100"
    })
    void shouldRejectNonIntegerOrOutOfRangeRadiusBeforeSearching(String radius) {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> useCase.findNearby(decimal("10.8"), decimal("106.7"), decimal(radius), 20));

        assertEquals("radiusKm must be an integer between 1 and 50", exception.getMessage());
        verifyNoInteractions(nearbyParkingLotPortOut, parkingMapMetricsPortOut);
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
                decimal("50000"),
                50
        )).thenReturn(List.of());

        assertEquals(List.of(), useCase.findNearby(
                decimal("90"),
                decimal("180"),
                decimal("50"),
                50
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "2", "3", "5", "7", "12", "49", "50", "1.00", "50.000", "1E+1" })
    void shouldAcceptIntegerRadiusFromOneToFiftyKilometres(String radius) {
        BigDecimal radiusKm = decimal(radius);
        BigDecimal meters = radiusKm.multiply(decimal("1000"));
        when(nearbyParkingLotPortOut.findNearby(decimal("10.8"), decimal("106.7"), meters, 20))
                .thenReturn(List.of());

        assertEquals(List.of(), useCase.findNearby(decimal("10.8"), decimal("106.7"), radiusKm, 20));
        verify(nearbyParkingLotPortOut).findNearby(decimal("10.8"), decimal("106.7"), meters, 20);
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
