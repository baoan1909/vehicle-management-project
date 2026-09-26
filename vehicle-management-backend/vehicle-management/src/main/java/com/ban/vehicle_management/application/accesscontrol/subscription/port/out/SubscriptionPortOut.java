package com.ban.vehicle_management.application.accesscontrol.subscription.port.out;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SubscriptionPortOut {

    Subscription save(Subscription subscription);

    Optional<Subscription> findById(UUID subscriptionId);

    Optional<Subscription> findActiveByCardId(UUID cardId, LocalDate businessDate);

    Optional<Subscription> findLatestActiveByCardId(UUID cardId);

    Optional<Subscription> findLatestPendingCardAssignmentByCardId(UUID cardId);

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

    List<Subscription> findAll(UUID customerId, UUID customerVehicleId, UUID cardId, UUID ticketTypeId,
            SubscriptionStatus status, LocalDate effectiveFrom, LocalDate effectiveTo,
            String keyword, Set<UUID> parkingLotIds);

    boolean existsOverlappingSubscription(
            UUID customerVehicleId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            UUID excludedSubscriptionId
    );

    boolean existsOverlappingSubscriptionInParkingLot(UUID customerVehicleId, UUID parkingLotId,
            LocalDate effectiveFrom, LocalDate effectiveTo, UUID excludedSubscriptionId);

    long countReservedOrActiveByVehicleTypeId(UUID vehicleTypeId);

    long countReservedOrActiveByVehicleTypeIdInParkingLot(UUID canonicalVehicleTypeId, UUID parkingLotId);

    Optional<Subscription> findActiveByLicensePlate(String licensePlate, LocalDate businessDate);

    Optional<Subscription> findActiveByLicensePlate(String licensePlate, LocalDate businessDate, Set<UUID> parkingLotIds);

    List<Subscription> findExpiredPendingPaymentsForUpdate(
            Instant approvedAtCutoff,
            LocalDate requestedEffectiveDateCutoff
    );

    int expireActiveSubscriptionsBefore(LocalDate businessDate);

    List<Subscription> findActiveSubscriptionsExpiringOn(LocalDate effectiveTo);

    List<Subscription> findActiveSubscriptionsExpiredBefore(LocalDate businessDate);
}
