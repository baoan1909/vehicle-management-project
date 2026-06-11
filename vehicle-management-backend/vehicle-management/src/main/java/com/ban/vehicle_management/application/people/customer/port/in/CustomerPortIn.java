package com.ban.vehicle_management.application.people.customer.port.in;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import java.util.List;
import java.util.UUID;

public interface CustomerPortIn {

    Customer getCustomerById(UUID customerId);

    List<Customer> getCustomers(
            CustomerStatus status,
            CustomerApprovalStatus approvalStatus,
            CustomerType customerType,
            String keyword
    );

    Customer activateCustomer(UUID customerId);

    Customer inactivateCustomer(UUID customerId);
}

