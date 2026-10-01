package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import com.ban.vehicle_management.application.parking.parkinglot.model.result.NearbyParkingLotResult;
import com.ban.vehicle_management.application.parking.parkinglot.port.in.NearbyParkingLotPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.NearbyParkingLotPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NearbyParkingLotUseCaseImpl implements NearbyParkingLotPortIn {

    static final BigDecimal MAX_RADIUS_KM = new BigDecimal("5");
    static final int MAX_LIMIT = 50;
    private static final BigDecimal METERS_PER_KILOMETER = new BigDecimal("1000");
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final NearbyParkingLotPortOut nearbyParkingLotPortOut;

    public NearbyParkingLotUseCaseImpl(NearbyParkingLotPortOut nearbyParkingLotPortOut) {
        this.nearbyParkingLotPortOut = nearbyParkingLotPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyParkingLotResult> findNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusKm,
            int limit
    ) {
        validateCoordinates(latitude, longitude);
        validateRadius(radiusKm);
        validateLimit(limit);
        return nearbyParkingLotPortOut.findNearby(
                latitude,
                longitude,
                radiusKm.multiply(METERS_PER_KILOMETER),
                limit
        );
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null) {
            throw new BadRequestException("latitude is required");
        }
        if (longitude == null) {
            throw new BadRequestException("longitude is required");
        }
        if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new BadRequestException("latitude must be between -90 and 90");
        }
        if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new BadRequestException("longitude must be between -180 and 180");
        }
    }

    private void validateRadius(BigDecimal radiusKm) {
        if (radiusKm == null) {
            throw new BadRequestException("radiusKm is required");
        }
        if (radiusKm.signum() <= 0 || radiusKm.compareTo(MAX_RADIUS_KM) > 0) {
            throw new BadRequestException("radiusKm must be greater than 0 and at most 5");
        }
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BadRequestException("limit must be between 1 and 50");
        }
    }
}
