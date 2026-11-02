package com.ban.vehicle_management.application.people.customerfavoriteparkinglot.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.people.customer.authorization.CustomerIdentityResolver;
import com.ban.vehicle_management.application.people.customerfavoriteparkinglot.port.out.CustomerFavoriteParkingLotPortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.people.customerfavoriteparkinglot.model.CustomerFavoriteParkingLot;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerFavoriteParkingLotUseCaseImplTest {

    @Mock CustomerFavoriteParkingLotPortOut favoriteParkingLotPortOut;
    @Mock CustomerIdentityResolver customerIdentityResolver;
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock ParkingLotPortOut parkingLotPortOut;

    private CustomerFavoriteParkingLotUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CustomerFavoriteParkingLotUseCaseImpl(
                favoriteParkingLotPortOut,
                customerIdentityResolver,
                currentAccountPortIn,
                parkingLotPortOut
        );
    }

    @Test
    void firstFavoriteCreatesOneActiveState() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        prepareIdentityAndParkingLot(customerId, accountId, parkingLotId);
        when(favoriteParkingLotPortOut.findByCustomerIdAndParkingLotId(customerId, parkingLotId))
                .thenReturn(Optional.empty());
        when(favoriteParkingLotPortOut.save(any(CustomerFavoriteParkingLot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerFavoriteParkingLot result = useCase.setMyFavoriteParkingLot(parkingLotId, true);

        assertTrue(result.isFavorite());
        assertTrue(result.getFavoritedAt() != null);
        assertNull(result.getUnfavoritedAt());
        verify(favoriteParkingLotPortOut).save(result);
    }

    @Test
    void secondClickDeactivatesExistingStateWithoutCreatingAnotherIdentity() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        CustomerFavoriteParkingLot existing = CustomerFavoriteParkingLot.create(
                customerId,
                parkingLotId,
                Instant.parse("2026-11-01T00:00:00Z")
        );
        prepareIdentityAndParkingLot(customerId, accountId, parkingLotId);
        when(favoriteParkingLotPortOut.findByCustomerIdAndParkingLotId(customerId, parkingLotId))
                .thenReturn(Optional.of(existing));
        when(favoriteParkingLotPortOut.save(existing)).thenReturn(existing);

        CustomerFavoriteParkingLot result = useCase.setMyFavoriteParkingLot(parkingLotId, false);

        assertSame(existing, result);
        assertFalse(result.isFavorite());
        assertTrue(result.getUnfavoritedAt() != null);
        verify(favoriteParkingLotPortOut).save(existing);
    }

    @Test
    void thirdClickReactivatesTheSameState() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        CustomerFavoriteParkingLot existing = CustomerFavoriteParkingLot.create(
                customerId,
                parkingLotId,
                Instant.parse("2026-11-01T00:00:00Z")
        );
        existing.changeFavorite(false, Instant.parse("2026-11-01T01:00:00Z"));
        prepareIdentityAndParkingLot(customerId, accountId, parkingLotId);
        when(favoriteParkingLotPortOut.findByCustomerIdAndParkingLotId(customerId, parkingLotId))
                .thenReturn(Optional.of(existing));
        when(favoriteParkingLotPortOut.save(existing)).thenReturn(existing);

        CustomerFavoriteParkingLot result = useCase.setMyFavoriteParkingLot(parkingLotId, true);

        assertSame(existing, result);
        assertTrue(result.isFavorite());
        assertNull(result.getUnfavoritedAt());
        verify(favoriteParkingLotPortOut).save(existing);
    }

    @Test
    void repeatedUnfavoriteWithoutAStoredStateIsIdempotent() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        prepareIdentityAndParkingLot(customerId, accountId, parkingLotId);
        when(favoriteParkingLotPortOut.findByCustomerIdAndParkingLotId(customerId, parkingLotId))
                .thenReturn(Optional.empty());

        CustomerFavoriteParkingLot result = useCase.setMyFavoriteParkingLot(parkingLotId, false);

        assertFalse(result.isFavorite());
        verify(favoriteParkingLotPortOut, never()).save(any());
    }

    @Test
    void favoriteListIsAlwaysScopedToTheCurrentApprovedCustomer() {
        UUID customerId = UUID.randomUUID();
        Set<UUID> expectedIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(customerIdentityResolver.resolveCurrentApprovedCustomerId()).thenReturn(customerId);
        when(favoriteParkingLotPortOut.findFavoriteParkingLotIdsByCustomerId(customerId))
                .thenReturn(expectedIds);

        Set<UUID> result = useCase.getMyFavoriteParkingLotIds();

        assertSame(expectedIds, result);
        verify(favoriteParkingLotPortOut).findFavoriteParkingLotIdsByCustomerId(customerId);
    }

    private void prepareIdentityAndParkingLot(UUID customerId, UUID accountId, UUID parkingLotId) {
        when(customerIdentityResolver.resolveCurrentApprovedCustomerId()).thenReturn(customerId);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(new ParkingLot()));
    }
}
