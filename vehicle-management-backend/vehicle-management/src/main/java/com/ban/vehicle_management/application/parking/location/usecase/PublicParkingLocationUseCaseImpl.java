package com.ban.vehicle_management.application.parking.location.usecase;

import com.ban.vehicle_management.application.parking.location.model.ParkingLocationSearchResult;
import com.ban.vehicle_management.application.parking.location.port.in.PublicParkingLocationPortIn;
import com.ban.vehicle_management.application.parking.location.port.in.ParkingLocationFeaturePortIn;
import com.ban.vehicle_management.application.parking.location.port.out.ParkingLocationPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublicParkingLocationUseCaseImpl implements PublicParkingLocationPortIn {

    private static final int MAX_QUERY_LENGTH = 200;

    private final ParkingLocationPortOut parkingLocationPortOut;
    private final ParkingLocationFeaturePortIn featurePortIn;

    public PublicParkingLocationUseCaseImpl(
            ParkingLocationPortOut parkingLocationPortOut,
            ParkingLocationFeaturePortIn featurePortIn
    ) {
        this.parkingLocationPortOut = parkingLocationPortOut;
        this.featurePortIn = featurePortIn;
    }

    @Override
    public List<ParkingLocationSearchResult> search(String query) {
        featurePortIn.requireCustomerParkingMap();
        featurePortIn.requireGeocodingProvider();
        String normalizedQuery = TextValidationUtils.normalizeRequiredText(
                query,
                "location query",
                MAX_QUERY_LENGTH
        );
        if (normalizedQuery.length() < 3) {
            throw new BadRequestException("location query must contain at least 3 characters");
        }
        return parkingLocationPortOut.search(normalizedQuery);
    }

    @Override
    public List<ParkingLocationSearchResult> reverse(BigDecimal latitude, BigDecimal longitude) {
        featurePortIn.requireCustomerParkingMap();
        featurePortIn.requireGeocodingProvider();
        if (latitude == null || longitude == null
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("parking location coordinates are invalid");
        }
        return parkingLocationPortOut.reverse(latitude, longitude);
    }
}
