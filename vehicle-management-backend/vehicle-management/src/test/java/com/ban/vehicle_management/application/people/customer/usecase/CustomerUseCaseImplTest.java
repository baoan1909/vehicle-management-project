package com.ban.vehicle_management.application.people.customer.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
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
    private UserProfileAvatarPortIn userProfileAvatarPortIn;

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
    void shouldResolveCustomerProfileAvatarWhenReturningCustomers() {
        UUID customerId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        Customer customer = validApprovedCustomer(customerId);
        customer.setUserProfile(profile(userProfileId, null));
        UserProfile resolvedProfile = profile(userProfileId, "https://cdn.example.com/customer-avatar.png");

        when(customerPortOut.findAll(null, null, null, null)).thenReturn(List.of(customer));
        when(userProfileAvatarPortIn.withResolvedAvatarUrls(List.of(customer.getUserProfile())))
                .thenReturn(List.of(resolvedProfile));

        List<Customer> customers = customerUseCase.getCustomers(null, null, null, null);

        assertEquals("https://cdn.example.com/customer-avatar.png",
                customers.getFirst().getUserProfile().getAvatarUrl());
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

    private UserProfile profile(UUID userProfileId, String avatarUrl) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("Nguyen Van Customer");
        userProfile.setAvatarUrl(avatarUrl);
        return userProfile;
    }

}
