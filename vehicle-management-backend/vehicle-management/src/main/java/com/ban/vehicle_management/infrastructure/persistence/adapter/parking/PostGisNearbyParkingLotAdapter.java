package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkinglot.model.result.NearbyParkingLotResult;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.NearbyParkingLotPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PostGisNearbyParkingLotAdapter implements NearbyParkingLotPortOut {

    private final ParkingLotRepository repository;

    public PostGisNearbyParkingLotAdapter(ParkingLotRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<NearbyParkingLotResult> findNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusMeters,
            int limit
    ) {
        return repository.findNearby(latitude, longitude, radiusMeters, limit).stream()
                .map(row -> new NearbyParkingLotResult(
                        row.getParkingLotId(), row.getName(), row.getAddressDisplay(), row.getLatitude(), row.getLongitude(),
                        row.getStatus(), row.getDistanceMeters()))
                .toList();
    }
}
