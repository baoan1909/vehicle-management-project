package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import com.ban.vehicle_management.application.parking.parkinglot.port.in.ParkingMapTelemetryPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingMapMetricsPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ParkingMapTelemetryUseCaseImpl implements ParkingMapTelemetryPortIn {

    private static final Set<String> ALLOWED_OUTCOMES = Set.of(
            "GRANTED",
            "DENIED",
            "TIMEOUT",
            "UNAVAILABLE",
            "UNSUPPORTED",
            "INSECURE_CONTEXT",
            "LOW_ACCURACY"
    );

    private final ParkingMapMetricsPortOut metricsPortOut;

    public ParkingMapTelemetryUseCaseImpl(ParkingMapMetricsPortOut metricsPortOut) {
        this.metricsPortOut = metricsPortOut;
    }

    @Override
    public void recordGeolocationOutcome(String outcome) {
        String normalized = outcome == null ? "" : outcome.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_OUTCOMES.contains(normalized)) {
            throw new BadRequestException("geolocation outcome is invalid");
        }
        metricsPortOut.recordGeolocationOutcome(normalized.toLowerCase(Locale.ROOT));
    }
}
