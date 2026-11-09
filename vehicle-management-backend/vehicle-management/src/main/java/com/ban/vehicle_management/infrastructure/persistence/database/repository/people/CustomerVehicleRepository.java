package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.util.Collection;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerVehicleRepository
        extends JpaRepository<CustomerVehicleEntity, UUID>, JpaSpecificationExecutor<CustomerVehicleEntity> {

    @Query("""
            select count(vehicle)
            from CustomerVehicleEntity vehicle
            join vehicle.customer customer
            join customer.userProfile profile
            join profile.account account
            where vehicle.createdAt >= :from and vehicle.createdAt < :to
              and customer.status <> com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.DELETED
              and account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
            """)
    long countDashboardNewVehicles(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select vehicle
            from CustomerVehicleEntity vehicle
            join vehicle.customer customer
            join customer.userProfile profile
            join profile.account account
            where vehicle.customerVehicleId = :customerVehicleId
              and customer.status <> com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.DELETED
              and profile.status <> com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus.DELETED
              and account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
            """)
    java.util.Optional<CustomerVehicleEntity> findBusinessVisibleById(
            @Param("customerVehicleId") UUID customerVehicleId);

    @Query("""
            select (count(vehicle) > 0)
            from CustomerVehicleEntity vehicle
            join vehicle.customer customer
            join customer.userProfile profile
            join profile.account account
            where vehicle.customerVehicleId = :customerVehicleId
              and customer.status <> com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.DELETED
              and profile.status <> com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus.DELETED
              and account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
              and (
                  exists (select subscription.subscriptionId from SubscriptionEntity subscription
                          where subscription.customerVehicleId = vehicle.customerVehicleId
                            and subscription.parkingLotId in :parkingLotIds)
                  or exists (select parkingSession.parkingSessionId from ParkingSessionEntity parkingSession
                             where parkingSession.customerVehicleId = vehicle.customerVehicleId
                               and parkingSession.parkingLotId in :parkingLotIds)
              )
            """)
    boolean existsBusinessVisibleInParkingLots(
            @Param("customerVehicleId") UUID customerVehicleId,
            @Param("parkingLotIds") java.util.Set<UUID> parkingLotIds);

    java.util.Optional<CustomerVehicleEntity> findByLicensePlateNormalized(String licensePlate);

    boolean existsByLicensePlateNormalized(String licensePlate);

    boolean existsByLicensePlateNormalizedAndCustomerVehicleIdNot(String licensePlate, UUID customerVehicleId);

    boolean existsByVehicleTypeIdAndStatusIn(UUID vehicleTypeId, Collection<CustomerVehicleStatus> statuses);

    long countByVehicleTypeIdAndStatusIn(UUID vehicleTypeId, Collection<CustomerVehicleStatus> statuses);

    List<CustomerVehicleEntity> findByCustomerIdAndIsDefaultTrue(UUID customerId);

    @Query("""
            select count(subscription) > 0
            from SubscriptionEntity subscription
            where subscription.customerVehicleId = :customerVehicleId
              and subscription.status = com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus.ACTIVE
            """)
    boolean existsActiveSubscription(@Param("customerVehicleId") UUID customerVehicleId);

    @Query("""
            select count(parkingSession) > 0
            from ParkingSessionEntity parkingSession
            where parkingSession.customerVehicleId = :customerVehicleId
              and parkingSession.status = com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus.OPEN
            """)
    boolean existsOpenParkingSession(@Param("customerVehicleId") UUID customerVehicleId);
}


