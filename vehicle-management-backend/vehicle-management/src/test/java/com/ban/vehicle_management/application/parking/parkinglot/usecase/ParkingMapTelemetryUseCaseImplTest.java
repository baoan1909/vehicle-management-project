package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingMapMetricsPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingMapTelemetryUseCaseImplTest {

    @Mock
    private ParkingMapMetricsPortOut metricsPortOut;

    @InjectMocks
    private ParkingMapTelemetryUseCaseImpl useCase;

    @Test
    void shouldRecordOnlyAllowlistedOutcomeWithoutCoordinates() {
        useCase.recordGeolocationOutcome(" denied ");

        verify(metricsPortOut).recordGeolocationOutcome("denied");
    }

    @Test
    void shouldRejectUnknownOutcome() {
        assertThrows(BadRequestException.class, () -> useCase.recordGeolocationOutcome("10.123456,106.123456"));
    }
}
