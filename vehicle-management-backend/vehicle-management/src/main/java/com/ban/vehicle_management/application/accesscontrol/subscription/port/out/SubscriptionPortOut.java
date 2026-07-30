package com.ban.vehicle_management.application.accesscontrol.subscription.port.out;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPortOut {

    Subscription save(Subscription subscription);

    Optional<Subscription> findById(UUID subscriptionId);

    Optional<Subscription> findActiveByCardId(UUID cardId, LocalDate businessDate);

    Optional<Subscription> findLatestActiveByCardId(UUID cardId);

    List<Subscription> findAll(
            UUID customerId,
            UUID customerVehicleId,
            UUID cardId,
            UUID ticketTypeId,
            SubscriptionStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String keyword
    );

    boolean existsOverlappingSubscription(
            UUID customerVehicleId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            UUID excludedSubscriptionId
    );

    long countReservedOrActiveByVehicleTypeId(UUID vehicleTypeId);

    Optional<Subscription> findActiveByLicensePlate(String licensePlate, LocalDate businessDate);

    int expireActiveSubscriptionsBefore(LocalDate businessDate);
}
