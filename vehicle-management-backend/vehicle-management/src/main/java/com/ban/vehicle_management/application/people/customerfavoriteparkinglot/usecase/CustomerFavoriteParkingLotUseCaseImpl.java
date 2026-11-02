package com.ban.vehicle_management.application.people.customerfavoriteparkinglot.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.people.customer.authorization.CustomerIdentityResolver;
import com.ban.vehicle_management.application.people.customerfavoriteparkinglot.port.in.CustomerFavoriteParkingLotPortIn;
import com.ban.vehicle_management.application.people.customerfavoriteparkinglot.port.out.CustomerFavoriteParkingLotPortOut;
import com.ban.vehicle_management.domain.people.customerfavoriteparkinglot.model.CustomerFavoriteParkingLot;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerFavoriteParkingLotUseCaseImpl implements CustomerFavoriteParkingLotPortIn {

    private final CustomerFavoriteParkingLotPortOut favoriteParkingLotPortOut;
    private final CustomerIdentityResolver customerIdentityResolver;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final ParkingLotPortOut parkingLotPortOut;

    public CustomerFavoriteParkingLotUseCaseImpl(
            CustomerFavoriteParkingLotPortOut favoriteParkingLotPortOut,
            CustomerIdentityResolver customerIdentityResolver,
            CurrentAccountPortIn currentAccountPortIn,
            ParkingLotPortOut parkingLotPortOut
    ) {
        this.favoriteParkingLotPortOut = favoriteParkingLotPortOut;
        this.customerIdentityResolver = customerIdentityResolver;
        this.currentAccountPortIn = currentAccountPortIn;
        this.parkingLotPortOut = parkingLotPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getMyFavoriteParkingLotIds() {
        UUID customerId = customerIdentityResolver.resolveCurrentApprovedCustomerId();
        return favoriteParkingLotPortOut.findFavoriteParkingLotIdsByCustomerId(customerId);
    }

    @Override
    @Transactional
    public CustomerFavoriteParkingLot setMyFavoriteParkingLot(UUID parkingLotId, boolean favorite) {
        UUID customerId = customerIdentityResolver.resolveCurrentApprovedCustomerId();
        UUID actorAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));

        Instant changedAt = Instant.now();
        CustomerFavoriteParkingLot favoriteParkingLot = favoriteParkingLotPortOut
                .findByCustomerIdAndParkingLotId(customerId, parkingLotId)
                .orElse(null);

        if (favoriteParkingLot == null) {
            if (!favorite) {
                return inactiveState(customerId, parkingLotId);
            }
            favoriteParkingLot = CustomerFavoriteParkingLot.create(customerId, parkingLotId, changedAt);
            favoriteParkingLot.setCreatedAt(changedAt);
            favoriteParkingLot.setCreatedBy(actorAccountId);
        } else {
            boolean previousState = favoriteParkingLot.isFavorite();
            favoriteParkingLot.changeFavorite(favorite, changedAt);
            if (previousState == favoriteParkingLot.isFavorite()) {
                return favoriteParkingLot;
            }
        }

        favoriteParkingLot.setUpdatedAt(changedAt);
        favoriteParkingLot.setUpdatedBy(actorAccountId);
        return favoriteParkingLotPortOut.save(favoriteParkingLot);
    }

    private CustomerFavoriteParkingLot inactiveState(UUID customerId, UUID parkingLotId) {
        CustomerFavoriteParkingLot favoriteParkingLot = new CustomerFavoriteParkingLot();
        favoriteParkingLot.setCustomerId(customerId);
        favoriteParkingLot.setParkingLotId(parkingLotId);
        favoriteParkingLot.setFavorite(false);
        return favoriteParkingLot;
    }
}

