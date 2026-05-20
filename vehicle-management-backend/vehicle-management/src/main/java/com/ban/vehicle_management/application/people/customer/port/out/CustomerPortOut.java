package com.ban.vehicle_management.application.people.customer.port.out;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerPortOut {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID customerId);

    List<Customer> findAll(
            CustomerStatus status,
            CustomerApprovalStatus approvalStatus,
            CustomerType customerType,
            String keyword
    );

    boolean existsByCustomerCode(String customerCode);

    boolean existsByCustomerCodeAndCustomerIdNot(String customerCode, UUID customerId);

    boolean existsByUserProfileId(UUID userProfileId);

    boolean existsByUserProfileIdAndCustomerIdNot(UUID userProfileId, UUID customerId);

    boolean existsUserProfileById(UUID userProfileId);
}

