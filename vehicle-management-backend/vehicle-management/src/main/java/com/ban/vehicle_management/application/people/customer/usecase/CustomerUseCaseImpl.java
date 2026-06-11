package com.ban.vehicle_management.application.people.customer.usecase;

import com.ban.vehicle_management.application.people.customer.port.in.CustomerPortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customer.policy.CustomerPolicy;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerUseCaseImpl implements CustomerPortIn {

    private final CustomerPortOut customerPortOut;
    private final CustomerPolicy customerPolicy = new CustomerPolicy();

    public CustomerUseCaseImpl(CustomerPortOut customerPortOut) {
        this.customerPortOut = customerPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerById(UUID customerId) {
        return customerPortOut.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getCustomers(
            CustomerStatus status,
            CustomerApprovalStatus approvalStatus,
            CustomerType customerType,
            String keyword
    ) {
        return customerPortOut.findAll(status, approvalStatus, customerType, keyword);
    }

    @Override
    @Transactional
    public Customer activateCustomer(UUID customerId) {
        Customer customer = getCustomerById(customerId);
        customerPolicy.activate(customer);
        return customerPortOut.save(customer);
    }

    @Override
    @Transactional
    public Customer inactivateCustomer(UUID customerId) {
        Customer customer = getCustomerById(customerId);
        customerPolicy.inactivate(customer);
        return customerPortOut.save(customer);
    }

}
