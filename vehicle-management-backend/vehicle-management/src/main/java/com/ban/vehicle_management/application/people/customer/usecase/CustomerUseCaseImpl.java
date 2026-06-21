package com.ban.vehicle_management.application.people.customer.usecase;

import com.ban.vehicle_management.application.people.customer.port.in.CustomerPortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customer.policy.CustomerPolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerUseCaseImpl implements CustomerPortIn {

    private final CustomerPortOut customerPortOut;
    private final UserProfileAvatarPortIn userProfileAvatarPortIn;
    private final CustomerPolicy customerPolicy = new CustomerPolicy();

    public CustomerUseCaseImpl(
            CustomerPortOut customerPortOut,
            UserProfileAvatarPortIn userProfileAvatarPortIn
    ) {
        this.customerPortOut = customerPortOut;
        this.userProfileAvatarPortIn = userProfileAvatarPortIn;
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerById(UUID customerId) {
        Customer customer = customerPortOut.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        return withResolvedAvatarUrl(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getCustomers(
            CustomerStatus status,
            CustomerApprovalStatus approvalStatus,
            CustomerType customerType,
            String keyword
    ) {
        return withResolvedAvatarUrls(customerPortOut.findAll(status, approvalStatus, customerType, keyword));
    }

    @Override
    @Transactional
    public Customer activateCustomer(UUID customerId) {
        Customer customer = getCustomerById(customerId);
        customerPolicy.activate(customer);
        return withResolvedAvatarUrl(customerPortOut.save(customer));
    }

    @Override
    @Transactional
    public Customer inactivateCustomer(UUID customerId) {
        Customer customer = getCustomerById(customerId);
        customerPolicy.inactivate(customer);
        return withResolvedAvatarUrl(customerPortOut.save(customer));
    }

    private Customer withResolvedAvatarUrl(Customer customer) {
        if (customer == null || customer.getUserProfile() == null) {
            return customer;
        }
        customer.setUserProfile(userProfileAvatarPortIn.withResolvedAvatarUrl(customer.getUserProfile()));
        return customer;
    }

    private List<Customer> withResolvedAvatarUrls(List<Customer> customers) {
        List<UserProfile> userProfiles = customers.stream()
                .map(Customer::getUserProfile)
                .filter(Objects::nonNull)
                .toList();
        if (userProfiles.isEmpty()) {
            return customers;
        }

        Map<UUID, UserProfile> resolvedProfilesById = userProfileAvatarPortIn.withResolvedAvatarUrls(userProfiles)
                .stream()
                .filter(Objects::nonNull)
                .filter(userProfile -> userProfile.getUserProfileId() != null)
                .collect(Collectors.toMap(UserProfile::getUserProfileId, userProfile -> userProfile));

        customers.forEach(customer -> {
            if (customer.getUserProfile() == null || customer.getUserProfile().getUserProfileId() == null) {
                return;
            }
            UserProfile resolvedProfile = resolvedProfilesById.get(customer.getUserProfile().getUserProfileId());
            if (resolvedProfile != null) {
                customer.setUserProfile(resolvedProfile);
            }
        });
        return customers;
    }

}
