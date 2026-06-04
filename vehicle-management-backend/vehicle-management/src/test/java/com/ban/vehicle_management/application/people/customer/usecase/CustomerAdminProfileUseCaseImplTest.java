package com.ban.vehicle_management.application.people.customer.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.people.customer.model.command.CreateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminVehicleDiffCommand;
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
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
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
    void shouldCreateCustomerAdminProfileWithVehicles() {
        UserProfile userProfile = validUserProfile();
        Customer customer = new Customer();
        customer.setCustomerType(CustomerType.VIP);

        UUID firstVehicleTypeId = UUID.randomUUID();
        UUID secondVehicleTypeId = UUID.randomUUID();
        CustomerVehicle firstCustomerVehicle = validVehicle(firstVehicleTypeId, "59A1-12345", Boolean.TRUE);
        CustomerVehicle secondCustomerVehicle = validVehicle(secondVehicleTypeId, "51B-67890", Boolean.FALSE);

        when(userProfilePortOut.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userProfilePortOut.existsByIdentifyCard("079123456789")).thenReturn(false);
        when(userProfilePortOut.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerPortOut.existsByCustomerCode(anyString())).thenReturn(false);
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerVehiclePortOut.existsVehicleTypeById(firstVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.existsVehicleTypeById(secondVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.findByLicensePlate("59A1-12345")).thenReturn(Optional.empty());
        when(customerVehiclePortOut.findByLicensePlate("51B-67890")).thenReturn(Optional.empty());
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerVehiclePortOut.findAll(any(UUID.class), isNull(), isNull(), isNull(), isNull())).thenAnswer(invocation ->
                List.of(
                        customerVehicleWithCustomer(
                                invocation.getArgument(0, UUID.class),
                                firstVehicleTypeId,
                                "59A1-12345",
                                Boolean.TRUE,
                                UUID.randomUUID()
                        ),
                        customerVehicleWithCustomer(
                                invocation.getArgument(0, UUID.class),
                                secondVehicleTypeId,
                                "51B-67890",
                                Boolean.FALSE,
                                UUID.randomUUID()
                        )
                ));

        CreateCustomerAdminProfileCommand command =
                new CreateCustomerAdminProfileCommand(
                        userProfile,
                        customer,
                        List.of(firstCustomerVehicle, secondCustomerVehicle)
                );

        CustomerAdminProfileResult result = customerAdminProfileUseCase.createCustomerAdminProfile(command);

        assertNotNull(result.userProfile().getUserProfileId());
        assertNotNull(result.customer().getCustomerId());
        assertEquals(result.userProfile().getUserProfileId(), result.customer().getUserProfileId());
        assertEquals(CustomerType.VIP, result.customer().getCustomerType());
        assertEquals(2, result.customerVehicles().size());
        assertEquals(result.customer().getCustomerId(), result.customerVehicles().get(0).getCustomerId());
    }

    @Test
    void shouldRejectCreateWhenMultipleVehiclesShareTheSameLicensePlate() {
        UserProfile userProfile = validUserProfile();
        Customer customer = new Customer();
        UUID firstVehicleTypeId = UUID.randomUUID();
        UUID secondVehicleTypeId = UUID.randomUUID();

        when(userProfilePortOut.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userProfilePortOut.existsByIdentifyCard("079123456789")).thenReturn(false);
        when(userProfilePortOut.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerPortOut.existsByCustomerCode(anyString())).thenReturn(false);
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerVehiclePortOut.existsVehicleTypeById(firstVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.existsVehicleTypeById(secondVehicleTypeId)).thenReturn(true);

        CreateCustomerAdminProfileCommand command = new CreateCustomerAdminProfileCommand(
                userProfile,
                customer,
                List.of(
                        validVehicle(firstVehicleTypeId, "59A1-12345", Boolean.FALSE),
                        validVehicle(secondVehicleTypeId, "59A1-12345", Boolean.TRUE)
                )
        );

        assertThrows(BadRequestException.class, () -> customerAdminProfileUseCase.createCustomerAdminProfile(command));
    }

    @Test
    void shouldRejectCreateWhenMultipleVehiclesAreMarkedDefault() {
        UserProfile userProfile = validUserProfile();
        Customer customer = new Customer();
        UUID firstVehicleTypeId = UUID.randomUUID();
        UUID secondVehicleTypeId = UUID.randomUUID();

        when(userProfilePortOut.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userProfilePortOut.existsByIdentifyCard("079123456789")).thenReturn(false);
        when(userProfilePortOut.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerPortOut.existsByCustomerCode(anyString())).thenReturn(false);
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerVehiclePortOut.existsVehicleTypeById(firstVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.existsVehicleTypeById(secondVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.findByLicensePlate("59A1-12345")).thenReturn(Optional.empty());
        when(customerVehiclePortOut.findByLicensePlate("51B-67890")).thenReturn(Optional.empty());

        CreateCustomerAdminProfileCommand command = new CreateCustomerAdminProfileCommand(
                userProfile,
                customer,
                List.of(
                        validVehicle(firstVehicleTypeId, "59A1-12345", Boolean.TRUE),
                        validVehicle(secondVehicleTypeId, "51B-67890", Boolean.TRUE)
                )
        );

        assertThrows(BadRequestException.class, () -> customerAdminProfileUseCase.createCustomerAdminProfile(command));
    }

    @Test
    void shouldUpdateCustomerAdminProfileAndExistingVehicle() {
        UUID customerId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID customerVehicleId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();

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

        CustomerVehicle existingVehicle =
                customerVehicleWithCustomer(customerId, vehicleTypeId, "59A1-12345", Boolean.FALSE, customerVehicleId);
        CustomerVehicle updatedVehicle = validVehicle(vehicleTypeId, "51B-67890", Boolean.TRUE);
        updatedVehicle.setCustomerVehicleId(customerVehicleId);

        CustomerVehicle finalUpdatedVehicle =
                customerVehicleWithCustomer(customerId, vehicleTypeId, "51B-67890", Boolean.TRUE, customerVehicleId);
        finalUpdatedVehicle.setBrand("Toyota");
        finalUpdatedVehicle.setColor("Black");

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(existingUserProfile));
        when(userProfilePortOut.existsByPhoneNumberAndUserProfileIdNot("0912345678", userProfileId)).thenReturn(false);
        when(userProfilePortOut.existsByIdentifyCardAndUserProfileIdNot("012345678901", userProfileId)).thenReturn(false);
        when(userProfilePortOut.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerPortOut.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerVehiclePortOut.existsVehicleTypeById(vehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.findByLicensePlate("51B-67890")).thenReturn(Optional.empty());
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerVehiclePortOut.findAll(customerId, null, null, null, null))
                .thenReturn(List.of(existingVehicle), List.of(finalUpdatedVehicle));

        UpdateCustomerAdminProfileCommand command = new UpdateCustomerAdminProfileCommand(
                updatedUserProfile,
                updatedCustomer,
                new UpdateCustomerAdminVehicleDiffCommand(List.of(), List.of(updatedVehicle), List.of())
        );

        CustomerAdminProfileResult result = customerAdminProfileUseCase.updateCustomerAdminProfile(customerId, command);

        assertEquals("Tran Thi B", result.userProfile().getFullName());
        assertEquals("0912345678", result.userProfile().getPhoneNumber());
        assertEquals(CustomerType.VIP, result.customer().getCustomerType());
        assertEquals("51B-67890", result.customerVehicles().get(0).getLicensePlate());
        assertEquals(Boolean.TRUE, result.customerVehicles().get(0).getIsDefault());
    }

    @Test
    void shouldAddNewVehicleWhenUpdatingCustomerAdminProfile() {
        UUID customerId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();

        Customer existingCustomer = validCustomer(customerId, userProfileId);
        UserProfile existingUserProfile = validUserProfile();
        existingUserProfile.setUserProfileId(userProfileId);

        CustomerVehicle newVehicle = validVehicle(vehicleTypeId, "59A1-12345", Boolean.FALSE);
        CustomerVehicle persistedVehicle =
                customerVehicleWithCustomer(customerId, vehicleTypeId, "59A1-12345", Boolean.FALSE, UUID.randomUUID());

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(existingUserProfile));
        when(customerVehiclePortOut.existsVehicleTypeById(vehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.findByLicensePlate("59A1-12345")).thenReturn(Optional.empty());
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerVehiclePortOut.findAll(customerId, null, null, null, null))
                .thenReturn(List.of(), List.of(persistedVehicle));

        UpdateCustomerAdminProfileCommand command = new UpdateCustomerAdminProfileCommand(
                null,
                null,
                new UpdateCustomerAdminVehicleDiffCommand(List.of(newVehicle), List.of(), List.of())
        );

        CustomerAdminProfileResult result = customerAdminProfileUseCase.updateCustomerAdminProfile(customerId, command);

        assertEquals(1, result.customerVehicles().size());
        assertEquals(customerId, result.customerVehicles().get(0).getCustomerId());
        assertEquals("59A1-12345", result.customerVehicles().get(0).getLicensePlate());
    }

    @Test
    void shouldRejectCreateWithLicensePlateOfVehicleBeingInactivated() {
        UUID customerId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID customerVehicleId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();

        Customer existingCustomer = validCustomer(customerId, userProfileId);
        UserProfile existingUserProfile = validUserProfile();
        existingUserProfile.setUserProfileId(userProfileId);
        CustomerVehicle existingVehicle =
                customerVehicleWithCustomer(customerId, vehicleTypeId, "59A1-12345", Boolean.FALSE, customerVehicleId);
        CustomerVehicle newVehicle = validVehicle(vehicleTypeId, "59A1-12345", Boolean.FALSE);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(existingUserProfile));
        when(customerVehiclePortOut.findAll(customerId, null, null, null, null)).thenReturn(List.of(existingVehicle));
        when(customerVehiclePortOut.existsVehicleTypeById(vehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.findByLicensePlate("59A1-12345")).thenReturn(Optional.of(existingVehicle));

        UpdateCustomerAdminProfileCommand command = new UpdateCustomerAdminProfileCommand(
                null,
                null,
                new UpdateCustomerAdminVehicleDiffCommand(
                        List.of(newVehicle),
                        List.of(),
                        List.of(customerVehicleId)
                )
        );

        assertThrows(
                ConflictException.class,
                () -> customerAdminProfileUseCase.updateCustomerAdminProfile(customerId, command)
        );
    }

    @Test
    void shouldAllowCreateWithOldLicensePlateWhenExistingVehicleIsUpdatedAwayFromIt() {
        UUID customerId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID existingVehicleId = UUID.randomUUID();
        UUID currentVehicleTypeId = UUID.randomUUID();
        UUID newVehicleTypeId = UUID.randomUUID();

        Customer existingCustomer = validCustomer(customerId, userProfileId);
        UserProfile existingUserProfile = validUserProfile();
        existingUserProfile.setUserProfileId(userProfileId);
        CustomerVehicle existingVehicle =
                customerVehicleWithCustomer(customerId, currentVehicleTypeId, "59A1-12345", Boolean.FALSE, existingVehicleId);

        CustomerVehicle updatedVehicle = validVehicle(currentVehicleTypeId, "51B-67890", Boolean.FALSE);
        updatedVehicle.setCustomerVehicleId(existingVehicleId);
        CustomerVehicle createdVehicle = validVehicle(newVehicleTypeId, "59A1-12345", Boolean.TRUE);

        CustomerVehicle finalUpdatedVehicle =
                customerVehicleWithCustomer(customerId, currentVehicleTypeId, "51B-67890", Boolean.FALSE, existingVehicleId);
        CustomerVehicle finalCreatedVehicle =
                customerVehicleWithCustomer(customerId, newVehicleTypeId, "59A1-12345", Boolean.TRUE, UUID.randomUUID());

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(existingUserProfile));
        when(customerVehiclePortOut.findAll(customerId, null, null, null, null))
                .thenReturn(List.of(existingVehicle), List.of(finalUpdatedVehicle, finalCreatedVehicle));
        when(customerVehiclePortOut.existsVehicleTypeById(currentVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.existsVehicleTypeById(newVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.findByLicensePlate("51B-67890")).thenReturn(Optional.empty());
        when(customerVehiclePortOut.findByLicensePlate("59A1-12345")).thenReturn(Optional.of(existingVehicle));
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCustomerAdminProfileCommand command = new UpdateCustomerAdminProfileCommand(
                null,
                null,
                new UpdateCustomerAdminVehicleDiffCommand(
                        List.of(createdVehicle),
                        List.of(updatedVehicle),
                        List.of()
                )
        );

        CustomerAdminProfileResult result = customerAdminProfileUseCase.updateCustomerAdminProfile(customerId, command);

        assertEquals(2, result.customerVehicles().size());
        assertEquals(List.of("51B-67890", "59A1-12345"),
                result.customerVehicles().stream().map(CustomerVehicle::getLicensePlate).sorted().toList());
    }

    @Test
    void shouldRejectVehicleActionOverlap() {
        UUID customerId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID customerVehicleId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();

        Customer existingCustomer = validCustomer(customerId, userProfileId);
        UserProfile existingUserProfile = validUserProfile();
        existingUserProfile.setUserProfileId(userProfileId);
        CustomerVehicle existingVehicle =
                customerVehicleWithCustomer(customerId, vehicleTypeId, "59A1-12345", Boolean.FALSE, customerVehicleId);
        CustomerVehicle updatedVehicle = validVehicle(vehicleTypeId, "51B-67890", Boolean.FALSE);
        updatedVehicle.setCustomerVehicleId(customerVehicleId);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(existingUserProfile));
        when(customerVehiclePortOut.findAll(customerId, null, null, null, null)).thenReturn(List.of(existingVehicle));

        UpdateCustomerAdminProfileCommand command = new UpdateCustomerAdminProfileCommand(
                null,
                null,
                new UpdateCustomerAdminVehicleDiffCommand(
                        List.of(),
                        List.of(updatedVehicle),
                        List.of(customerVehicleId)
                )
        );

        assertThrows(
                BadRequestException.class,
                () -> customerAdminProfileUseCase.updateCustomerAdminProfile(customerId, command)
        );
    }

    @Test
    void shouldRejectMultipleDefaultVehiclesAfterApplyingDiff() {
        UUID customerId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID existingVehicleId = UUID.randomUUID();
        UUID currentVehicleTypeId = UUID.randomUUID();
        UUID newVehicleTypeId = UUID.randomUUID();

        Customer existingCustomer = validCustomer(customerId, userProfileId);
        UserProfile existingUserProfile = validUserProfile();
        existingUserProfile.setUserProfileId(userProfileId);
        CustomerVehicle existingVehicle =
                customerVehicleWithCustomer(customerId, currentVehicleTypeId, "59A1-12345", Boolean.FALSE, existingVehicleId);

        CustomerVehicle updatedVehicle = validVehicle(currentVehicleTypeId, "51B-67890", Boolean.TRUE);
        updatedVehicle.setCustomerVehicleId(existingVehicleId);
        CustomerVehicle createdVehicle = validVehicle(newVehicleTypeId, "60A1-12345", Boolean.TRUE);

        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(existingUserProfile));
        when(customerVehiclePortOut.findAll(customerId, null, null, null, null)).thenReturn(List.of(existingVehicle));
        when(customerVehiclePortOut.existsVehicleTypeById(currentVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.existsVehicleTypeById(newVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.findByLicensePlate("51B-67890")).thenReturn(Optional.empty());
        when(customerVehiclePortOut.findByLicensePlate("60A1-12345")).thenReturn(Optional.empty());

        UpdateCustomerAdminProfileCommand command = new UpdateCustomerAdminProfileCommand(
                null,
                null,
                new UpdateCustomerAdminVehicleDiffCommand(
                        List.of(createdVehicle),
                        List.of(updatedVehicle),
                        List.of()
                )
        );

        assertThrows(
                BadRequestException.class,
                () -> customerAdminProfileUseCase.updateCustomerAdminProfile(customerId, command)
        );
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

    private CustomerVehicle validVehicle(UUID vehicleTypeId, String licensePlate, Boolean isDefault) {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setVehicleTypeId(vehicleTypeId);
        customerVehicle.setLicensePlate(licensePlate);
        customerVehicle.setBrand("Honda");
        customerVehicle.setColor("White");
        customerVehicle.setIsDefault(isDefault);
        return customerVehicle;
    }

    private CustomerVehicle customerVehicleWithCustomer(
            UUID customerId,
            UUID vehicleTypeId,
            String licensePlate,
            Boolean isDefault,
            UUID customerVehicleId
    ) {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerVehicleId(customerVehicleId);
        customerVehicle.setCustomerId(customerId);
        customerVehicle.setVehicleTypeId(vehicleTypeId);
        customerVehicle.setLicensePlate(licensePlate);
        customerVehicle.setBrand("Honda");
        customerVehicle.setColor("White");
        customerVehicle.setIsDefault(isDefault);
        customerVehicle.setStatus(CustomerVehicleStatus.ACTIVE);
        return customerVehicle;
    }
}
