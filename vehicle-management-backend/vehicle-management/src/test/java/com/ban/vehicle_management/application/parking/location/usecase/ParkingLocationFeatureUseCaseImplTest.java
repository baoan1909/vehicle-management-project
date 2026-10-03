package com.ban.vehicle_management.application.parking.location.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.location.model.ParkingLocationFeatureStatus;
import com.ban.vehicle_management.application.parking.location.port.out.ParkingLocationFeaturePortOut;
import com.ban.vehicle_management.shared.exception.FeatureDisabledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingLocationFeatureUseCaseImplTest {

    @Mock
    private ParkingLocationFeaturePortOut featurePortOut;

    private ParkingLocationFeatureUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ParkingLocationFeatureUseCaseImpl(featurePortOut);
    }

    @Test
    void shouldExposeOnlyPublicFeatureStatus() {
        when(featurePortOut.isCustomerParkingMapEnabled()).thenReturn(true);
        when(featurePortOut.isPublicNearbySearchEnabled()).thenReturn(true);
        when(featurePortOut.isGeocodingProviderEnabled()).thenReturn(false);

        assertEquals(new ParkingLocationFeatureStatus(true, true, false), useCase.getPublicStatus());
    }

    @Test
    void shouldRejectEveryDisabledFeatureWithStableFeatureName() {
        assertDisabled(useCase::requireAdminAddressV2, "ADMIN_ADDRESS_V2_ENABLED");
        assertDisabled(useCase::requirePublicNearbySearch, "PUBLIC_NEARBY_SEARCH_ENABLED");
        assertDisabled(useCase::requireCustomerParkingMap, "CUSTOMER_PARKING_MAP_ENABLED");
        assertDisabled(useCase::requireGeocodingProvider, "GEOCODING_PROVIDER_ENABLED");
    }

    private void assertDisabled(Runnable action, String expectedFeature) {
        FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, action::run);
        assertEquals(expectedFeature, exception.feature());
    }
}