package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID>, JpaSpecificationExecutor<CustomerEntity> {

    boolean existsByCustomerCode(String customerCode);

    boolean existsByCustomerCodeAndCustomerIdNot(String customerCode, UUID customerId);

    boolean existsByUserProfileId(UUID userProfileId);

    boolean existsByUserProfileIdAndCustomerIdNot(UUID userProfileId, UUID customerId);
}


