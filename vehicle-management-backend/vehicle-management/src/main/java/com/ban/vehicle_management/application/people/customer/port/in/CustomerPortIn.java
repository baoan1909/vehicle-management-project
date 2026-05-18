package com.ban.vehicle_management.application.people.customer.port.in;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.shared.enumeration.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.CustomerType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CustomerPortIn {

    Customer createCustomer(Customer customer);

    Customer updateCustomer(UUID customerId, Customer customer);

    Customer getCustomerById(UUID customerId);

    List<Customer> getCustomers(CustomerApprovalStatus approvalStatus, CustomerType customerType, String keyword);

    Customer approveCustomer(UUID customerId, UUID approvedBy, Instant approvedAt);

    Customer rejectCustomer(UUID customerId);

    Customer suspendCustomer(UUID customerId);

    Customer moveCustomerToPending(UUID customerId);
}
