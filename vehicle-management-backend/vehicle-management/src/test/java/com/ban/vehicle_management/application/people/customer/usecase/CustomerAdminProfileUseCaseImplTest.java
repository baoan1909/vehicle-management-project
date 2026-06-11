package com.ban.vehicle_management.application.people.customer.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.result.CustomerAdminProfileResult;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerAdminProfileUseCaseImplTest {

    @Mock
    private UserProfilePortOut userProfilePortOut;

    @Mock
    private CustomerPortOut customerPortOut;

    @Mock
    private CustomerVehiclePortOut customerVehiclePortOut;

    @InjectMocks
    private CustomerAdminProfileUseCaseImpl customerAdminProfileUseCase;

    @Test
    void shouldUpdateCustomerAdminProfileWithoutChangingVehicles() {
        UUID customerId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        UserProfile existingUserProfile = validUserProfile();
        existingUserProfile.setUserProfileId(userProfileId);
        UserProfile updatedUserProfile = validUserProfile();
        updatedUserProfile.setFullName("Tran Thi B");
        updatedUserProfile.setPhoneNumber("0912345678");
        updatedUserProfile.setIdentifyCard("012345678901");
        updatedUserProfile.setStatus(UserProfileStatus.SUSPENDED);

        Customer existingCustomer = validCustomer(customerId, userProfileId);
        Customer updatedCustomer = new Customer();
        updatedCustomer.setCustomerType(CustomerType.VIP);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(existingUserProfile));
        when(userProfilePortOut.existsByPhoneNumberAndUserProfileIdNot("0912345678", userProfileId)).thenReturn(false);
        when(userProfilePortOut.existsByIdentifyCardAndUserProfileIdNot("012345678901", userProfileId)).thenReturn(false);
        when(userProfilePortOut.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerVehiclePortOut.findAll(customerId, null, null, null, null))
                .thenReturn(List.of());

        UpdateCustomerAdminProfileCommand command = new UpdateCustomerAdminProfileCommand(
                updatedUserProfile,
                updatedCustomer
        );

        CustomerAdminProfileResult result = customerAdminProfileUseCase.updateCustomerAdminProfile(customerId, command);

        assertEquals("Tran Thi B", result.userProfile().getFullName());
        assertEquals("0912345678", result.userProfile().getPhoneNumber());
        assertEquals(CustomerType.VIP, result.customer().getCustomerType());
        assertEquals(0, result.customerVehicles().size());
    }

    private UserProfile validUserProfile() {
        UserProfile userProfile = new UserProfile();
        userProfile.setFullName("Nguyen Van A");
        userProfile.setDateOfBirth(LocalDate.of(1995, 1, 10));
        userProfile.setGender("male");
        userProfile.setPhoneNumber("0901234567");
        userProfile.setAddress("Ho Chi Minh City");
        userProfile.setIdentifyCard("079123456789");
        userProfile.setAvatarUrl("https://example.com/avatar.jpg");
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        return userProfile;
    }

    private Customer validCustomer(UUID customerId, UUID userProfileId) {
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setUserProfileId(userProfileId);
        customer.setCustomerCode("CUS-001");
        customer.setCustomerType(CustomerType.REGISTERED);
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.PENDING);
        return customer;
    }

}
