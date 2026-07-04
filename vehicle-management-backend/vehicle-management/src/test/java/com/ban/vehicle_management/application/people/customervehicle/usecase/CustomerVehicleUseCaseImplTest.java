package com.ban.vehicle_management.application.people.customervehicle.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.people.customervehicle.authorization.CustomerVehicleAccessGuard;
import com.ban.vehicle_management.application.people.customervehicle.model.command.CustomerVehicleBatchCommand;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerVehicleUseCaseImplTest {

    @Mock
    private CustomerVehiclePortOut customerVehiclePortOut;

    @Mock
    private CustomerVehicleAccessGuard customerVehicleAccessGuard;

    @InjectMocks
    private CustomerVehicleUseCaseImpl customerVehicleUseCase;

    @Test
    void shouldCreateCustomerVehicleWithDefaultActiveStatus() {
        CustomerVehicle requestCustomerVehicle = new CustomerVehicle();
        requestCustomerVehicle.setCustomerId(UUID.randomUUID());
        requestCustomerVehicle.setVehicleTypeId(UUID.randomUUID());
        requestCustomerVehicle.setLicensePlate(" 51a-12345 ");

        when(customerVehicleAccessGuard.resolveCustomerIdForCreate(requestCustomerVehicle.getCustomerId()))
                .thenReturn(requestCustomerVehicle.getCustomerId());
        when(customerVehiclePortOut.existsCustomerById(requestCustomerVehicle.getCustomerId())).thenReturn(true);
        when(customerVehiclePortOut.existsVehicleTypeById(requestCustomerVehicle.getVehicleTypeId())).thenReturn(true);
        when(customerVehiclePortOut.existsByLicensePlate("51a-12345")).thenReturn(false);
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerVehicle createdCustomerVehicle = customerVehicleUseCase.createCustomerVehicle(requestCustomerVehicle);

        assertEquals("51a-12345", createdCustomerVehicle.getLicensePlate());
        assertEquals(Boolean.FALSE, createdCustomerVehicle.getIsDefault());
        assertEquals(CustomerVehicleStatus.ACTIVE, createdCustomerVehicle.getStatus());
    }

    @Test
    void shouldCreateCustomerVehicleForCurrentApprovedCustomerWhenOnlyOwnPermissionIsGranted() {
        UUID currentCustomerId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();

        CustomerVehicle requestCustomerVehicle = new CustomerVehicle();
        requestCustomerVehicle.setCustomerId(UUID.randomUUID());
        requestCustomerVehicle.setVehicleTypeId(vehicleTypeId);
        requestCustomerVehicle.setLicensePlate("59A-12345");

        when(customerVehicleAccessGuard.resolveCustomerIdForCreate(requestCustomerVehicle.getCustomerId()))
                .thenReturn(currentCustomerId);
        when(customerVehiclePortOut.existsCustomerById(currentCustomerId)).thenReturn(true);
        when(customerVehiclePortOut.existsVehicleTypeById(vehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.existsByLicensePlate("59A-12345")).thenReturn(false);
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerVehicle createdCustomerVehicle = customerVehicleUseCase.createCustomerVehicle(requestCustomerVehicle);

        assertEquals(currentCustomerId, createdCustomerVehicle.getCustomerId());
    }

    @Test
    void shouldRejectCreateWhenCustomerDoesNotExist() {
        CustomerVehicle requestCustomerVehicle = new CustomerVehicle();
        requestCustomerVehicle.setCustomerId(UUID.randomUUID());
        requestCustomerVehicle.setVehicleTypeId(UUID.randomUUID());
        requestCustomerVehicle.setLicensePlate("51A-12345");

        when(customerVehicleAccessGuard.resolveCustomerIdForCreate(requestCustomerVehicle.getCustomerId()))
                .thenReturn(requestCustomerVehicle.getCustomerId());
        when(customerVehiclePortOut.existsCustomerById(requestCustomerVehicle.getCustomerId())).thenReturn(false);

        assertThrows(NotFoundException.class, () -> customerVehicleUseCase.createCustomerVehicle(requestCustomerVehicle));
        verify(customerVehiclePortOut, never()).save(any(CustomerVehicle.class));
    }

    @Test
    void shouldRejectCreateWhenVehicleTypeDoesNotExist() {
        CustomerVehicle requestCustomerVehicle = new CustomerVehicle();
        requestCustomerVehicle.setCustomerId(UUID.randomUUID());
        requestCustomerVehicle.setVehicleTypeId(UUID.randomUUID());
        requestCustomerVehicle.setLicensePlate("51A-12345");

        when(customerVehicleAccessGuard.resolveCustomerIdForCreate(requestCustomerVehicle.getCustomerId()))
                .thenReturn(requestCustomerVehicle.getCustomerId());
        when(customerVehiclePortOut.existsCustomerById(requestCustomerVehicle.getCustomerId())).thenReturn(true);
        when(customerVehiclePortOut.existsVehicleTypeById(requestCustomerVehicle.getVehicleTypeId())).thenReturn(false);

        assertThrows(NotFoundException.class, () -> customerVehicleUseCase.createCustomerVehicle(requestCustomerVehicle));
    }

    @Test
    void shouldRejectDuplicateLicensePlateOnCreate() {
        CustomerVehicle requestCustomerVehicle = new CustomerVehicle();
        requestCustomerVehicle.setCustomerId(UUID.randomUUID());
        requestCustomerVehicle.setVehicleTypeId(UUID.randomUUID());
        requestCustomerVehicle.setLicensePlate("51A-12345");

        when(customerVehicleAccessGuard.resolveCustomerIdForCreate(requestCustomerVehicle.getCustomerId()))
                .thenReturn(requestCustomerVehicle.getCustomerId());
        when(customerVehiclePortOut.existsCustomerById(requestCustomerVehicle.getCustomerId())).thenReturn(true);
        when(customerVehiclePortOut.existsVehicleTypeById(requestCustomerVehicle.getVehicleTypeId())).thenReturn(true);
        when(customerVehiclePortOut.existsByLicensePlate("51A-12345")).thenReturn(true);

        assertThrows(ConflictException.class, () -> customerVehicleUseCase.createCustomerVehicle(requestCustomerVehicle));
    }

    @Test
    void shouldUpdateCustomerVehicleMetadata() {
        UUID customerVehicleId = UUID.randomUUID();
        CustomerVehicle existingCustomerVehicle = validCustomerVehicle(customerVehicleId);

        CustomerVehicle requestCustomerVehicle = new CustomerVehicle();
        requestCustomerVehicle.setVehicleTypeId(UUID.randomUUID());
        requestCustomerVehicle.setLicensePlate("51B-67890");
        requestCustomerVehicle.setBrand("Toyota");
        requestCustomerVehicle.setColor("Black");
        requestCustomerVehicle.setIsDefault(Boolean.TRUE);

        when(customerVehiclePortOut.findById(customerVehicleId)).thenReturn(Optional.of(existingCustomerVehicle));
        when(customerVehiclePortOut.existsVehicleTypeById(requestCustomerVehicle.getVehicleTypeId())).thenReturn(true);
        when(customerVehiclePortOut.existsByLicensePlateAndCustomerVehicleIdNot("51B-67890", customerVehicleId))
                .thenReturn(false);
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerVehiclePortOut.findDefaultVehiclesByCustomerId(existingCustomerVehicle.getCustomerId()))
                .thenReturn(List.of(existingCustomerVehicle));

        CustomerVehicle updatedCustomerVehicle =
                customerVehicleUseCase.updateCustomerVehicle(customerVehicleId, requestCustomerVehicle);

        assertEquals("51B-67890", updatedCustomerVehicle.getLicensePlate());
        assertEquals("Toyota", updatedCustomerVehicle.getBrand());
        assertEquals("Black", updatedCustomerVehicle.getColor());
        assertEquals(Boolean.TRUE, updatedCustomerVehicle.getIsDefault());
    }

    @Test
    void shouldApplyCustomerVehicleBatchWithCreateUpdateAndInactivate() {
        UUID customerId = UUID.randomUUID();
        UUID vehicleToUpdateId = UUID.randomUUID();
        UUID vehicleToInactivateId = UUID.randomUUID();
        UUID existingVehicleTypeId = UUID.randomUUID();
        UUID newVehicleTypeId = UUID.randomUUID();

        CustomerVehicle existingVehicleToUpdate =
                customerVehicleWithCustomer(customerId, existingVehicleTypeId, "59A1-12345", Boolean.FALSE, vehicleToUpdateId);
        CustomerVehicle existingVehicleToInactivate =
                customerVehicleWithCustomer(customerId, existingVehicleTypeId, "60A1-12345", Boolean.FALSE, vehicleToInactivateId);

        CustomerVehicle requestedVehicleUpdate = new CustomerVehicle();
        requestedVehicleUpdate.setCustomerVehicleId(vehicleToUpdateId);
        requestedVehicleUpdate.setVehicleTypeId(existingVehicleTypeId);
        requestedVehicleUpdate.setLicensePlate("51B-67890");
        requestedVehicleUpdate.setBrand("Toyota");
        requestedVehicleUpdate.setColor("Black");
        requestedVehicleUpdate.setIsDefault(Boolean.FALSE);

        CustomerVehicle requestedVehicleCreate = new CustomerVehicle();
        requestedVehicleCreate.setVehicleTypeId(newVehicleTypeId);
        requestedVehicleCreate.setLicensePlate("61C-11111");
        requestedVehicleCreate.setBrand("Honda");
        requestedVehicleCreate.setColor("White");
        requestedVehicleCreate.setIsDefault(Boolean.TRUE);

        CustomerVehicle persistedCreatedVehicle =
                customerVehicleWithCustomer(customerId, newVehicleTypeId, "61C-11111", Boolean.TRUE, UUID.randomUUID());
        CustomerVehicle persistedUpdatedVehicle =
                customerVehicleWithCustomer(customerId, existingVehicleTypeId, "51B-67890", Boolean.FALSE, vehicleToUpdateId);

        when(customerVehicleAccessGuard.resolveCustomerIdForCreate(customerId)).thenReturn(customerId);
        when(customerVehiclePortOut.existsCustomerById(customerId)).thenReturn(true);
        when(customerVehiclePortOut.findAll(customerId, null, null, null, null))
                .thenReturn(
                        List.of(existingVehicleToUpdate, existingVehicleToInactivate),
                        List.of(persistedUpdatedVehicle, persistedCreatedVehicle)
                );
        when(customerVehiclePortOut.existsVehicleTypeById(existingVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.existsVehicleTypeById(newVehicleTypeId)).thenReturn(true);
        when(customerVehiclePortOut.findByLicensePlate("51B-67890")).thenReturn(Optional.empty());
        when(customerVehiclePortOut.findByLicensePlate("61C-11111")).thenReturn(Optional.empty());
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerVehicleBatchCommand command = new CustomerVehicleBatchCommand(
                customerId,
                List.of(requestedVehicleCreate),
                List.of(requestedVehicleUpdate),
                List.of(vehicleToInactivateId)
        );

        List<CustomerVehicle> result = customerVehicleUseCase.applyCustomerVehicleBatch(command);

        assertEquals(2, result.size());
        assertEquals(List.of("51B-67890", "61C-11111"),
                result.stream().map(CustomerVehicle::getLicensePlate).sorted().toList());
        verify(customerVehicleAccessGuard).ensureCanUpdate(any(CustomerVehicle.class));
        verify(customerVehicleAccessGuard).ensureCanActivateOrInactivate(any(CustomerVehicle.class));
        verify(customerVehiclePortOut, times(3)).save(any(CustomerVehicle.class));
    }

    @Test
    void shouldRejectBatchWhenSameVehicleIsUpdatedAndInactivated() {
        UUID customerId = UUID.randomUUID();
        UUID customerVehicleId = UUID.randomUUID();
        CustomerVehicle existingVehicle = validCustomerVehicle(customerVehicleId);
        existingVehicle.setCustomerId(customerId);

        CustomerVehicle requestedVehicleUpdate = new CustomerVehicle();
        requestedVehicleUpdate.setCustomerVehicleId(customerVehicleId);
        requestedVehicleUpdate.setVehicleTypeId(existingVehicle.getVehicleTypeId());
        requestedVehicleUpdate.setLicensePlate("51B-67890");

        when(customerVehicleAccessGuard.resolveCustomerIdForRead(customerId)).thenReturn(customerId);
        when(customerVehiclePortOut.existsCustomerById(customerId)).thenReturn(true);
        when(customerVehiclePortOut.findAll(customerId, null, null, null, null)).thenReturn(List.of(existingVehicle));

        CustomerVehicleBatchCommand command = new CustomerVehicleBatchCommand(
                customerId,
                List.of(),
                List.of(requestedVehicleUpdate),
                List.of(customerVehicleId)
        );

        assertThrows(BadRequestException.class, () -> customerVehicleUseCase.applyCustomerVehicleBatch(command));
    }

    @Test
    void shouldReturnFilteredCustomerVehicles() {
        UUID customerId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        when(customerVehiclePortOut.findAll(
                customerId,
                CustomerVehicleStatus.ACTIVE,
                vehicleTypeId,
                Boolean.TRUE,
                "nguyen"
        )).thenReturn(List.of(new CustomerVehicle(), new CustomerVehicle()));
        when(customerVehicleAccessGuard.resolveCustomerIdForRead(customerId)).thenReturn(customerId);

        List<CustomerVehicle> customerVehicles = customerVehicleUseCase.getAllCustomerVehicle(
                customerId,
                CustomerVehicleStatus.ACTIVE,
                vehicleTypeId,
                Boolean.TRUE,
                "nguyen"
        );

        assertEquals(2, customerVehicles.size());
        verify(customerVehiclePortOut).findAll(customerId, CustomerVehicleStatus.ACTIVE, vehicleTypeId, Boolean.TRUE, "nguyen");
    }

    @Test
    void shouldForceListQueryToCurrentApprovedCustomerWhenOnlyOwnReadPermissionIsGranted() {
        UUID currentCustomerId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();

        when(customerVehicleAccessGuard.resolveCustomerIdForRead(any(UUID.class))).thenReturn(currentCustomerId);
        when(customerVehiclePortOut.findAll(
                currentCustomerId,
                CustomerVehicleStatus.ACTIVE,
                vehicleTypeId,
                Boolean.FALSE,
                "abc"
        )).thenReturn(List.of(new CustomerVehicle()));

        List<CustomerVehicle> customerVehicles = customerVehicleUseCase.getAllCustomerVehicle(
                UUID.randomUUID(),
                CustomerVehicleStatus.ACTIVE,
                vehicleTypeId,
                Boolean.FALSE,
                "abc"
        );

        assertEquals(1, customerVehicles.size());
        verify(customerVehiclePortOut).findAll(
                currentCustomerId,
                CustomerVehicleStatus.ACTIVE,
                vehicleTypeId,
                Boolean.FALSE,
                "abc"
        );
    }

    @Test
    void shouldSoftDeleteCustomerVehicleBySettingInactiveStatus() {
        UUID customerVehicleId = UUID.randomUUID();
        CustomerVehicle existingCustomerVehicle = validCustomerVehicle(customerVehicleId);
        existingCustomerVehicle.setIsDefault(Boolean.TRUE);

        when(customerVehiclePortOut.findById(customerVehicleId)).thenReturn(Optional.of(existingCustomerVehicle));
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customerVehicleUseCase.deleteCustomerVehicle(customerVehicleId);

        assertEquals(CustomerVehicleStatus.INACTIVE, existingCustomerVehicle.getStatus());
        assertEquals(Boolean.FALSE, existingCustomerVehicle.getIsDefault());
        verify(customerVehicleAccessGuard).ensureCanDelete(existingCustomerVehicle);
        verify(customerVehiclePortOut).save(existingCustomerVehicle);
    }

    @Test
    void shouldAllowCustomerToInactivateOwnedVehicle() {
        UUID customerVehicleId = UUID.randomUUID();
        CustomerVehicle customerVehicle = validCustomerVehicle(customerVehicleId);

        when(customerVehiclePortOut.findById(customerVehicleId)).thenReturn(Optional.of(customerVehicle));
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerVehicle inactivatedCustomerVehicle = customerVehicleUseCase.inactivateCustomerVehicle(customerVehicleId);

        assertEquals(CustomerVehicleStatus.INACTIVE, inactivatedCustomerVehicle.getStatus());
        verify(customerVehicleAccessGuard).ensureCanActivateOrInactivate(customerVehicle);
    }

    @Test
    void shouldBlockCustomerVehicle() {
        UUID customerVehicleId = UUID.randomUUID();
        CustomerVehicle customerVehicle = validCustomerVehicle(customerVehicleId);
        customerVehicle.setIsDefault(Boolean.TRUE);

        when(customerVehiclePortOut.findById(customerVehicleId)).thenReturn(Optional.of(customerVehicle));
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerVehicle blockedCustomerVehicle = customerVehicleUseCase.blockCustomerVehicle(customerVehicleId);

        assertEquals(CustomerVehicleStatus.BLOCKED, blockedCustomerVehicle.getStatus());
        assertEquals(Boolean.FALSE, blockedCustomerVehicle.getIsDefault());
        verify(customerVehicleAccessGuard).ensureCanBlock();
    }

    @Test
    void shouldMarkCustomerVehicleAsDefaultAndUnmarkOthers() {
        UUID customerVehicleId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CustomerVehicle targetCustomerVehicle = validCustomerVehicle(customerVehicleId);
        targetCustomerVehicle.setCustomerId(customerId);

        CustomerVehicle existingDefaultVehicle = validCustomerVehicle(UUID.randomUUID());
        existingDefaultVehicle.setCustomerId(customerId);
        existingDefaultVehicle.setIsDefault(Boolean.TRUE);

        when(customerVehiclePortOut.findById(customerVehicleId)).thenReturn(Optional.of(targetCustomerVehicle));
        when(customerVehiclePortOut.save(any(CustomerVehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerVehiclePortOut.findDefaultVehiclesByCustomerId(customerId))
                .thenReturn(List.of(targetCustomerVehicle, existingDefaultVehicle));

        CustomerVehicle defaultCustomerVehicle = customerVehicleUseCase.markCustomerVehicleAsDefault(customerVehicleId);

        assertEquals(Boolean.TRUE, defaultCustomerVehicle.getIsDefault());
        assertEquals(Boolean.FALSE, existingDefaultVehicle.getIsDefault());
        verify(customerVehicleAccessGuard).ensureCanUpdate(targetCustomerVehicle);
        verify(customerVehiclePortOut, times(2)).save(any(CustomerVehicle.class));
    }

    @Test
    void shouldDelegateReadAuthorizationToAccessGuard() {
        UUID customerVehicleId = UUID.randomUUID();
        CustomerVehicle customerVehicle = validCustomerVehicle(customerVehicleId);
        when(customerVehiclePortOut.findById(customerVehicleId)).thenReturn(Optional.of(customerVehicle));

        CustomerVehicle result = customerVehicleUseCase.getCustomerVehicleById(customerVehicleId);

        assertEquals(customerVehicle, result);
        verify(customerVehicleAccessGuard).ensureCanRead(customerVehicle);
    }

    @Test
    void shouldThrowWhenCustomerVehicleDoesNotExist() {
        UUID customerVehicleId = UUID.randomUUID();
        when(customerVehiclePortOut.findById(customerVehicleId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> customerVehicleUseCase.getCustomerVehicleById(customerVehicleId));
    }

    private CustomerVehicle validCustomerVehicle(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerVehicleId(customerVehicleId);
        customerVehicle.setCustomerId(UUID.randomUUID());
        customerVehicle.setVehicleTypeId(UUID.randomUUID());
        customerVehicle.setLicensePlate("51A-12345");
        customerVehicle.setBrand("Honda");
        customerVehicle.setColor("White");
        customerVehicle.setIsDefault(Boolean.FALSE);
        customerVehicle.setStatus(CustomerVehicleStatus.ACTIVE);
        return customerVehicle;
    }

    private CustomerVehicle customerVehicleWithCustomer(
            UUID customerId,
            UUID vehicleTypeId,
            String licensePlate,
            Boolean isDefault,
            UUID customerVehicleId
    ) {
        CustomerVehicle customerVehicle = validCustomerVehicle(customerVehicleId);
        customerVehicle.setCustomerId(customerId);
        customerVehicle.setVehicleTypeId(vehicleTypeId);
        customerVehicle.setLicensePlate(licensePlate);
        customerVehicle.setIsDefault(isDefault);
        return customerVehicle;
    }
}
