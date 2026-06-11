package com.ban.vehicle_management.application.people.customer.usecase;

import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.result.CustomerAdminProfileResult;
import com.ban.vehicle_management.application.people.customer.port.in.CustomerAdminProfilePortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customer.policy.CustomerPolicy;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAdminProfileUseCaseImpl implements CustomerAdminProfilePortIn {

    private final UserProfilePortOut userProfilePortOut;
    private final CustomerPortOut customerPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final UserProfilePolicy userProfilePolicy = new UserProfilePolicy();
    private final CustomerPolicy customerPolicy = new CustomerPolicy();

    public CustomerAdminProfileUseCaseImpl(
            UserProfilePortOut userProfilePortOut,
            CustomerPortOut customerPortOut,
            CustomerVehiclePortOut customerVehiclePortOut
    ) {
        this.userProfilePortOut = userProfilePortOut;
        this.customerPortOut = customerPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
    }

    @Override
    @Transactional
    public CustomerAdminProfileResult updateCustomerAdminProfile(UUID customerId, UpdateCustomerAdminProfileCommand command) {
        ensureUpdatePayloadHasContent(command);

        Customer existingCustomer = customerPortOut.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        UserProfile existingUserProfile = userProfilePortOut.findById(existingCustomer.getUserProfileId())
                .orElseThrow(() -> new NotFoundException("User profile not found"));

        if (hasUserProfileChanges(command.userProfile())) {
            applyUserProfileChanges(existingUserProfile, command.userProfile());
            userProfilePolicy.validateState(existingUserProfile);
            validateUniqueUserProfile(existingUserProfile, existingUserProfile.getUserProfileId());
            existingUserProfile = userProfilePortOut.save(existingUserProfile);
        }

        if (hasCustomerChanges(command.customer())) {
            applyCustomerChanges(existingCustomer, command.customer());
            customerPolicy.validateState(existingCustomer);
            existingCustomer = customerPortOut.save(existingCustomer);
        }

        return buildResult(existingUserProfile, existingCustomer);
    }

    private void ensureUpdatePayloadHasContent(UpdateCustomerAdminProfileCommand command) {
        if (command == null || (!hasUserProfileChanges(command.userProfile())
                && !hasCustomerChanges(command.customer()))) {
            throw new BadRequestException("At least one profile field or customer field must be provided");
        }
    }

    private void validateUniqueUserProfile(UserProfile userProfile, UUID userProfileId) {
        if (userProfile.getPhoneNumber() != null
                && userProfilePortOut.existsByPhoneNumberAndUserProfileIdNot(userProfile.getPhoneNumber(), userProfileId)) {
            throw new ConflictException("User profile phone number already exists");
        }
        if (userProfile.getIdentifyCard() != null
                && userProfilePortOut.existsByIdentifyCardAndUserProfileIdNot(userProfile.getIdentifyCard(), userProfileId)) {
            throw new ConflictException("User profile identify card already exists");
        }
    }

    private void applyUserProfileChanges(UserProfile existingUserProfile, UserProfile updatedUserProfile) {
        existingUserProfile.setFullName(updatedUserProfile.getFullName());
        existingUserProfile.setDateOfBirth(updatedUserProfile.getDateOfBirth());
        existingUserProfile.setGender(updatedUserProfile.getGender());
        existingUserProfile.setPhoneNumber(updatedUserProfile.getPhoneNumber());
        existingUserProfile.setAddress(updatedUserProfile.getAddress());
        existingUserProfile.setIdentifyCard(updatedUserProfile.getIdentifyCard());
        existingUserProfile.setAvatarUrl(updatedUserProfile.getAvatarUrl());
        if (updatedUserProfile.getStatus() != null) {
            existingUserProfile.setStatus(updatedUserProfile.getStatus());
        }
    }

    private void applyCustomerChanges(Customer existingCustomer, Customer updatedCustomer) {
        if (updatedCustomer.getCustomerType() != null) {
            existingCustomer.setCustomerType(updatedCustomer.getCustomerType());
        }
    }

    private boolean hasUserProfileChanges(UserProfile userProfile) {
        return userProfile != null
                && (userProfile.getFullName() != null
                || userProfile.getDateOfBirth() != null
                || userProfile.getGender() != null
                || userProfile.getPhoneNumber() != null
                || userProfile.getAddress() != null
                || userProfile.getIdentifyCard() != null
                || userProfile.getAvatarUrl() != null
                || userProfile.getStatus() != null);
    }

    private boolean hasCustomerChanges(Customer customer) {
        return customer != null && customer.getCustomerType() != null;
    }

    private CustomerAdminProfileResult buildResult(UserProfile userProfile, Customer customer) {
        List<CustomerVehicle> customerVehicles =
                customerVehiclePortOut.findAll(customer.getCustomerId(), null, null, null, null);
        return new CustomerAdminProfileResult(userProfile, customer, customerVehicles);
    }

}
