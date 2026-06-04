package com.ban.vehicle_management.domain.people.customer.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerPolicyTest {

    private final CustomerPolicy customerPolicy = new CustomerPolicy();

    @Test
    void shouldInitializeCustomerWithDefaults() {
        Customer customer = new Customer();
        customer.setUserProfileId(UUID.randomUUID());
        customer.setCustomerCode(" CUS-001 ");

        customerPolicy.initialize(customer);

        assertEquals("CUS-001", customer.getCustomerCode());
        assertEquals(CustomerType.REGISTERED, customer.getCustomerType());
        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
        assertEquals(CustomerApprovalStatus.PENDING, customer.getApprovalStatus());
    }

    @Test
    void shouldApprovePendingCustomer() {
        Customer customer = validPendingCustomer();
        customer.setStatus(CustomerStatus.INACTIVE);
        UUID approvedBy = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2026-05-15T02:00:00Z");

        customerPolicy.approve(customer, approvedBy, approvedAt);

        assertEquals(CustomerApprovalStatus.APPROVED, customer.getApprovalStatus());
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
        assertEquals(approvedBy, customer.getApprovedBy());
        assertEquals(approvedAt, customer.getApprovedAt());
    }

    @Test
    void shouldRejectApprovedMetadataOutsideApprovedStatus() {
        Customer customer = validPendingCustomer();
        customer.setApprovedBy(UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> customerPolicy.validateState(customer));
    }

    @Test
    void shouldRejectApprovedCustomer() {
        Customer customer = validApprovedCustomer();

        customerPolicy.reject(customer);

        assertEquals(CustomerApprovalStatus.REJECTED, customer.getApprovalStatus());
        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
        assertNull(customer.getApprovedBy());
        assertNull(customer.getApprovedAt());
    }

    @Test
    void shouldInactivateCustomer() {
        Customer customer = validPendingCustomer();

        customerPolicy.inactivate(customer);

        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
    }

    @Test
    void shouldActivateCustomer() {
        Customer customer = validApprovedCustomer();
        customer.setStatus(CustomerStatus.INACTIVE);

        customerPolicy.activate(customer);

        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
    }

    @Test
    void shouldRejectActivatingPendingCustomer() {
        Customer customer = validPendingCustomer();

        assertThrows(BadRequestException.class, () -> customerPolicy.activate(customer));
    }

    private Customer validPendingCustomer() {
        Customer customer = new Customer();
        customer.setUserProfileId(UUID.randomUUID());
        customer.setCustomerCode("CUS-001");
        customer.setCustomerType(CustomerType.REGISTERED);
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.PENDING);
        return customer;
    }

    private Customer validApprovedCustomer() {
        Customer customer = validPendingCustomer();
        customer.setApprovalStatus(CustomerApprovalStatus.APPROVED);
        customer.setApprovedBy(UUID.randomUUID());
        customer.setApprovedAt(Instant.parse("2026-05-15T02:00:00Z"));
        return customer;
    }
}

