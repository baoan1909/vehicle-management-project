package com.ban.vehicle_management.application.parking.location.usecase;

import com.ban.vehicle_management.application.parking.location.model.ParkingLocationFeatureStatus;
import com.ban.vehicle_management.application.parking.location.port.in.ParkingLocationFeaturePortIn;
import com.ban.vehicle_management.application.parking.location.port.out.ParkingLocationFeaturePortOut;
import com.ban.vehicle_management.shared.exception.FeatureDisabledException;
import org.springframework.stereotype.Service;

@Service
public class ParkingLocationFeatureUseCaseImpl implements ParkingLocationFeaturePortIn {

    static final String ADMIN_ADDRESS_V2 = "ADMIN_ADDRESS_V2_ENABLED";
    static final String PUBLIC_NEARBY_SEARCH = "PUBLIC_NEARBY_SEARCH_ENABLED";
    static final String CUSTOMER_PARKING_MAP = "CUSTOMER_PARKING_MAP_ENABLED";
    static final String GEOCODING_PROVIDER = "GEOCODING_PROVIDER_ENABLED";

    private final ParkingLocationFeaturePortOut featurePortOut;

    public ParkingLocationFeatureUseCaseImpl(ParkingLocationFeaturePortOut featurePortOut) {
        this.featurePortOut = featurePortOut;
    }

    @Override
    public ParkingLocationFeatureStatus getPublicStatus() {
        return new ParkingLocationFeatureStatus(
                featurePortOut.isCustomerParkingMapEnabled(),
                featurePortOut.isPublicNearbySearchEnabled(),
                featurePortOut.isGeocodingProviderEnabled()
        );
    }

    @Override
    public void requireAdminAddressV2() {
        require(featurePortOut.isAdminAddressV2Enabled(), ADMIN_ADDRESS_V2);
    }

    @Override
    public void requirePublicNearbySearch() {
        require(featurePortOut.isPublicNearbySearchEnabled(), PUBLIC_NEARBY_SEARCH);
    }

    @Override
    public void requireCustomerParkingMap() {
        require(featurePortOut.isCustomerParkingMapEnabled(), CUSTOMER_PARKING_MAP);
    }

    @Override
    public void requireGeocodingProvider() {
        require(featurePortOut.isGeocodingProviderEnabled(), GEOCODING_PROVIDER);
    }

    private void require(boolean enabled, String feature) {
        if (!enabled) {
            throw new FeatureDisabledException(feature);
        }
    }
}