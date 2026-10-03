package com.ban.vehicle_management.infrastructure.config.feature;

import com.ban.vehicle_management.application.parking.location.port.out.ParkingLocationFeaturePortOut;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.features.parking-location")
public class ParkingLocationFeatureConfigurationAdapter implements ParkingLocationFeaturePortOut {

    private boolean adminAddressV2Enabled;
    private boolean publicNearbySearchEnabled;
    private boolean customerParkingMapEnabled;
    private boolean geocodingProviderEnabled;

    @AssertTrue(message = "Customer parking map requires public nearby search")
    public boolean isValid() {
        return !customerParkingMapEnabled || publicNearbySearchEnabled;
    }
}