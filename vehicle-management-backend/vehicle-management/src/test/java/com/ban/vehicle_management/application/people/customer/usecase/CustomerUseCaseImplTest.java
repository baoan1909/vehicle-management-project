package com.ban.vehicle_management.application.people.customer.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.shared.enumeration.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.CustomerType;
import com.ban.vehicle_management.shared.exception.ConflictException;
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

    @InjectMocks
    private CustomerUseCaseImpl customerUseCase;

    @Test
    void shouldCreateCustomerWithDefaults() {
        Customer requestCustomer = new Customer();
        requestCustomer.setUserProfileId(UUID.randomUUID());
        requestCustomer.setCustomerCode(" cus-001 ");

        when(customerPortOut.existsUserProfileById(requestCustomer.getUserProfileId())).thenReturn(true);
        when(customerPortOut.existsByCustomerCode("CUS-001")).thenReturn(false);
        when(customerPortOut.existsByUserProfileId(requestCustomer.getUserProfileId())).thenReturn(false);
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer createdCustomer = customerUseCase.createCustomer(requestCustomer);

        assertEquals("CUS-001", createdCustomer.getCustomerCode());
        assertEquals(CustomerType.REGISTERED, createdCustomer.getCustomerType());
        assertEquals(CustomerApprovalStatus.PENDING, createdCustomer.getApprovalStatus());
    }

    @Test
    void shouldRejectCreateWhenUserProfileDoesNotExist() {
        Customer requestCustomer = new Customer();
        requestCustomer.setUserProfileId(UUID.randomUUID());
        requestCustomer.setCustomerCode("CUS-001");

        when(customerPortOut.existsUserProfileById(requestCustomer.getUserProfileId())).thenReturn(false);

        assertThrows(NotFoundException.class, () -> customerUseCase.createCustomer(requestCustomer));
        verify(customerPortOut, never()).save(any(Customer.class));
    }

    @Test
    void shouldRejectDuplicateCustomerCodeOnCreate() {
        Customer requestCustomer = new Customer();
        requestCustomer.setUserProfileId(UUID.randomUUID());
        requestCustomer.setCustomerCode("CUS-001");

        when(customerPortOut.existsUserProfileById(requestCustomer.getUserProfileId())).thenReturn(true);
        when(customerPortOut.existsByCustomerCode("CUS-001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> customerUseCase.createCustomer(requestCustomer));
    }

    @Test
    void shouldRejectLinkedUserProfileOnCreate() {
        Customer requestCustomer = new Customer();
        requestCustomer.setUserProfileId(UUID.randomUUID());
        requestCustomer.setCustomerCode("CUS-001");

        when(customerPortOut.existsUserProfileById(requestCustomer.getUserProfileId())).thenReturn(true);
        when(customerPortOut.existsByCustomerCode("CUS-001")).thenReturn(false);
        when(customerPortOut.existsByUserProfileId(requestCustomer.getUserProfileId())).thenReturn(true);

        assertThrows(ConflictException.class, () -> customerUseCase.createCustomer(requestCustomer));
    }

    @Test
    void shouldUpdateCustomerMetadata() {
        UUID customerId = UUID.randomUUID();
        Customer existingCustomer = new Customer();
        existingCustomer.setCustomerId(customerId);
        existingCustomer.setUserProfileId(UUID.randomUUID());
        existingCustomer.setCustomerCode("CUS-001");
        existingCustomer.setCustomerType(CustomerType.REGISTERED);
        existingCustomer.setApprovalStatus(CustomerApprovalStatus.PENDING);

        Customer requestCustomer = new Customer();
        requestCustomer.setCustomerCode("VIP-001");
        requestCustomer.setCustomerType(CustomerType.VIP);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerPortOut.existsByCustomerCodeAndCustomerIdNot("VIP-001", customerId)).thenReturn(false);
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer updatedCustomer = customerUseCase.updateCustomer(customerId, requestCustomer);

        assertEquals("VIP-001", updatedCustomer.getCustomerCode());
        assertEquals(CustomerType.VIP, updatedCustomer.getCustomerType());
        assertEquals(CustomerApprovalStatus.PENDING, updatedCustomer.getApprovalStatus());
    }

    @Test
    void shouldReturnFilteredCustomers() {
        when(customerPortOut.findAll(CustomerApprovalStatus.PENDING, CustomerType.REGISTERED, "cus"))
                .thenReturn(List.of(new Customer(), new Customer()));

        List<Customer> customers = customerUseCase.getCustomers(CustomerApprovalStatus.PENDING, CustomerType.REGISTERED, "cus");

        assertEquals(2, customers.size());
        verify(customerPortOut).findAll(CustomerApprovalStatus.PENDING, CustomerType.REGISTERED, "cus");
    }

    @Test
    void shouldApproveCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID approvedBy = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2026-05-17T03:00:00Z");
        Customer customer = validPendingCustomer(customerId);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer approvedCustomer = customerUseCase.approveCustomer(customerId, approvedBy, approvedAt);

        assertEquals(CustomerApprovalStatus.APPROVED, approvedCustomer.getApprovalStatus());
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
    }

    @Test
    void shouldSuspendCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = validApprovedCustomer(customerId);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer suspendedCustomer = customerUseCase.suspendCustomer(customerId);

        assertEquals(CustomerApprovalStatus.SUSPENDED, suspendedCustomer.getApprovalStatus());
    }

    @Test
    void shouldMoveCustomerToPending() {
        UUID customerId = UUID.randomUUID();
        Customer customer = validApprovedCustomer(customerId);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer pendingCustomer = customerUseCase.moveCustomerToPending(customerId);

        assertEquals(CustomerApprovalStatus.PENDING, pendingCustomer.getApprovalStatus());
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
