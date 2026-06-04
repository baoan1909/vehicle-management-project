package com.ban.vehicle_management.application.people.customer.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.customer.port.in.CustomerPortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customer.policy.CustomerPolicy;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerUseCaseImpl implements CustomerPortIn {

    private final CustomerPortOut customerPortOut;
    private final CustomerPolicy customerPolicy = new CustomerPolicy();
    private final CurrentAccountPortIn currentAccountPortIn;

    public CustomerUseCaseImpl(CustomerPortOut customerPortOut, CurrentAccountPortIn currentAccountPortIn) {
        this.customerPortOut = customerPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
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
    public Customer approveCustomer(UUID customerId, Instant approvedAt) {
        Customer customer = getCustomerById(customerId);
        UUID approvedBy = currentAccountPortIn.getCurrentAccountIdOrThrow();
        customerPolicy.approve(customer, approvedBy, approvedAt == null ? Instant.now() : approvedAt);
        return customerPortOut.save(customer);
    }

    @Override
    @Transactional
    public Customer rejectCustomer(UUID customerId) {
        Customer customer = getCustomerById(customerId);
        customerPolicy.reject(customer);
        return customerPortOut.save(customer);
    }

    @Override
    @Transactional
    public Customer suspendCustomer(UUID customerId) {
        Customer customer = getCustomerById(customerId);
        customerPolicy.suspend(customer);
        return customerPortOut.save(customer);
    }

    @Override
    @Transactional
    public Customer moveCustomerToPending(UUID customerId) {
        Customer customer = getCustomerById(customerId);
        customerPolicy.moveToPending(customer);
        return customerPortOut.save(customer);
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

