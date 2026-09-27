package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerVehicleRepository
        extends JpaRepository<CustomerVehicleEntity, UUID>, JpaSpecificationExecutor<CustomerVehicleEntity> {

    java.util.Optional<CustomerVehicleEntity> findByLicensePlateNormalized(String licensePlate);

    boolean existsByLicensePlateNormalized(String licensePlate);

    boolean existsByLicensePlateNormalizedAndCustomerVehicleIdNot(String licensePlate, UUID customerVehicleId);

    boolean existsByVehicleTypeIdAndStatusIn(UUID vehicleTypeId, Collection<CustomerVehicleStatus> statuses);

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


