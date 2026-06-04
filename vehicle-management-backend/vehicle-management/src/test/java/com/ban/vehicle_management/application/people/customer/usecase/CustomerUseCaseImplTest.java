package com.ban.vehicle_management.application.people.customer.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerUseCaseImplTest {

    @Mock
    private CustomerPortOut customerPortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks
    private CustomerUseCaseImpl customerUseCase;

    @Test
    void shouldReturnFilteredCustomers() {
        when(customerPortOut.findAll(CustomerStatus.ACTIVE, CustomerApprovalStatus.PENDING, CustomerType.REGISTERED, "cus"))
                .thenReturn(List.of(new Customer(), new Customer()));

        List<Customer> customers = customerUseCase.getCustomers(
                CustomerStatus.ACTIVE,
                CustomerApprovalStatus.PENDING,
                CustomerType.REGISTERED,
                "cus"
        );

        assertEquals(2, customers.size());
        verify(customerPortOut).findAll(CustomerStatus.ACTIVE, CustomerApprovalStatus.PENDING, CustomerType.REGISTERED, "cus");
    }

    @Test
    void shouldApproveCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID approvedBy = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2026-05-17T03:00:00Z");
        Customer customer = validPendingCustomer(customerId);
        customer.setStatus(CustomerStatus.INACTIVE);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(approvedBy);
        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer approvedCustomer = customerUseCase.approveCustomer(customerId, approvedAt);

        assertEquals(CustomerApprovalStatus.APPROVED, approvedCustomer.getApprovalStatus());
        assertEquals(CustomerStatus.ACTIVE, approvedCustomer.getStatus());
        assertEquals(approvedBy, approvedCustomer.getApprovedBy());
        assertEquals(approvedAt, approvedCustomer.getApprovedAt());
    }

    @Test
    void shouldRejectCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = validApprovedCustomer(customerId);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer rejectedCustomer = customerUseCase.rejectCustomer(customerId);

        assertEquals(CustomerApprovalStatus.REJECTED, rejectedCustomer.getApprovalStatus());
        assertEquals(CustomerStatus.INACTIVE, rejectedCustomer.getStatus());
    }

    @Test
    void shouldSuspendCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = validApprovedCustomer(customerId);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer suspendedCustomer = customerUseCase.suspendCustomer(customerId);

        assertEquals(CustomerApprovalStatus.SUSPENDED, suspendedCustomer.getApprovalStatus());
        assertEquals(CustomerStatus.INACTIVE, suspendedCustomer.getStatus());
    }

    @Test
    void shouldMoveCustomerToPending() {
        UUID customerId = UUID.randomUUID();
        Customer customer = validApprovedCustomer(customerId);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer pendingCustomer = customerUseCase.moveCustomerToPending(customerId);

        assertEquals(CustomerApprovalStatus.PENDING, pendingCustomer.getApprovalStatus());
        assertEquals(CustomerStatus.INACTIVE, pendingCustomer.getStatus());
    }

    @Test
    void shouldActivateCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = validApprovedCustomer(customerId);
        customer.setStatus(CustomerStatus.INACTIVE);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer activatedCustomer = customerUseCase.activateCustomer(customerId);

        assertEquals(CustomerStatus.ACTIVE, activatedCustomer.getStatus());
    }

    @Test
    void shouldInactivateCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = validPendingCustomer(customerId);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer inactivatedCustomer = customerUseCase.inactivateCustomer(customerId);

        assertEquals(CustomerStatus.INACTIVE, inactivatedCustomer.getStatus());
    }

    @Test
    void shouldThrowWhenCustomerDoesNotExist() {
        UUID customerId = UUID.randomUUID();
        when(customerPortOut.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> customerUseCase.getCustomerById(customerId));
    }

    private Customer validPendingCustomer(UUID customerId) {
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setUserProfileId(UUID.randomUUID());
        customer.setCustomerCode("CUS-001");
        customer.setCustomerType(CustomerType.REGISTERED);
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.PENDING);
        return customer;
    }

    private Customer validApprovedCustomer(UUID customerId) {
        Customer customer = validPendingCustomer(customerId);
        customer.setApprovalStatus(CustomerApprovalStatus.APPROVED);
        customer.setApprovedBy(UUID.randomUUID());
        customer.setApprovedAt(Instant.parse("2026-05-17T03:00:00Z"));
        return customer;
    }
}
