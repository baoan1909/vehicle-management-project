package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID>, JpaSpecificationExecutor<CustomerEntity> {

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
        """)
    Optional<UUID> findAccountIdByCustomerId(@Param("customerId") UUID customerId);
}


