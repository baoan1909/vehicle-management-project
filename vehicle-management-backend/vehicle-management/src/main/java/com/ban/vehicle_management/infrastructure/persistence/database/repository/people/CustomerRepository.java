package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID>, JpaSpecificationExecutor<CustomerEntity> {

    @Query("""
            select count(customer)
            from CustomerEntity customer
            join customer.userProfile profile
            join profile.account account
            where customer.createdAt >= :from and customer.createdAt < :to
              and customer.status <> com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.DELETED
              and account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
            """)
    long countDashboardNewCustomers(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select customer
            from CustomerEntity customer
            join customer.userProfile profile
            join profile.account account
            where customer.customerId = :customerId
              and customer.status <> com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.DELETED
              and profile.status <> com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus.DELETED
              and account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
            """)
    Optional<CustomerEntity> findBusinessVisibleById(@Param("customerId") UUID customerId);

    @Query("""
            select (count(customer) > 0)
            from CustomerEntity customer
            join customer.userProfile profile
            join profile.account account
            where customer.customerId = :customerId
              and customer.status <> com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.DELETED
              and profile.status <> com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus.DELETED
              and account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
            """)
    boolean existsBusinessVisibleById(@Param("customerId") UUID customerId);

    @Query("""
            select (count(customer) > 0)
            from CustomerEntity customer
            join customer.userProfile profile
            join profile.account account
            where customer.customerId = :customerId
              and customer.status <> com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.DELETED
              and profile.status <> com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus.DELETED
              and account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
              and (
                  exists (select subscription.subscriptionId from SubscriptionEntity subscription
                          where subscription.customerId = customer.customerId
                            and subscription.parkingLotId in :parkingLotIds)
                  or exists (select parkingSession.parkingSessionId from ParkingSessionEntity parkingSession
                             where parkingSession.customerId = customer.customerId
                               and parkingSession.parkingLotId in :parkingLotIds)
              )
            """)
    boolean existsBusinessVisibleInParkingLots(
            @Param("customerId") UUID customerId,
            @Param("parkingLotIds") java.util.Set<UUID> parkingLotIds);

    boolean existsByCustomerCode(String customerCode);

    boolean existsByCustomerCodeAndCustomerIdNot(String customerCode, UUID customerId);

    boolean existsByUserProfileId(UUID userProfileId);

    boolean existsByUserProfileIdAndCustomerIdNot(UUID userProfileId, UUID customerId);

    Optional<CustomerEntity> findByUserProfileId(UUID userProfileId);

    @Query("""
        SELECT account.accountId
        FROM CustomerEntity customer
        JOIN AccountEntity account
          ON account.userProfileId = customer.userProfileId
        WHERE customer.customerId = :customerId
          AND customer.status <> com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.DELETED
          AND account.status NOT IN (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
        """)
    Optional<UUID> findAccountIdByCustomerId(@Param("customerId") UUID customerId);

    @Query("""
        SELECT customer.customerId
        FROM CustomerEntity customer
        JOIN AccountEntity account
          ON account.userProfileId = customer.userProfileId
        WHERE account.accountId = :accountId
          AND customer.status <> com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.DELETED
          AND account.status NOT IN (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
        """)
    Optional<UUID> findCustomerIdByAccountId(@Param("accountId") UUID accountId);

    @Query(value = """
        select customer.customer_id from iam.accounts account
        join people.customers customer on customer.user_profile_id=account.user_profile_id
        where account.account_id=:accountId and customer.status<>'DELETED'
        limit 1
        """, nativeQuery = true)
    Optional<UUID> findDeletionCustomerIdByAccountId(@Param("accountId") UUID accountId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
        update people.customers set customer_code=:marker, status='DELETED', approved_by=null, updated_at=:now
        where user_profile_id=:profileId and status<>'DELETED'
        """, nativeQuery = true)
    int tombstoneByUserProfileId(
            @Param("profileId") UUID profileId,
            @Param("marker") String marker,
            @Param("now") Instant now);
}


