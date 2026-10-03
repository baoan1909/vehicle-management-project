package com.ban.vehicle_management.infrastructure.config.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParkingLocationFeatureConfigurationAdapterTest {

    @Test
    void shouldRequireNearbySearchWhenCustomerMapIsEnabled() {
        ParkingLocationFeatureConfigurationAdapter properties = new ParkingLocationFeatureConfigurationAdapter();
        properties.setCustomerParkingMapEnabled(true);
        properties.setPublicNearbySearchEnabled(false);

        assertFalse(properties.isValid());

        properties.setPublicNearbySearchEnabled(true);
        assertTrue(properties.isValid());
    }

    @Test
    void shouldAllowSafeAllDisabledDeployment() {
        assertTrue(new ParkingLocationFeatureConfigurationAdapter().isValid());
    }
}