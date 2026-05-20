package com.ban.vehicle_management.application.people.customervehicle.usecase;

import com.ban.vehicle_management.application.people.customervehicle.port.in.CustomerVehiclePortIn;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.domain.people.customervehicle.policy.CustomerVehiclePolicy;
import com.ban.vehicle_management.shared.enumeration.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerVehicleUseCaseImpl implements CustomerVehiclePortIn {

    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final CustomerVehiclePolicy customerVehiclePolicy = new CustomerVehiclePolicy();

    public CustomerVehicleUseCaseImpl(CustomerVehiclePortOut customerVehiclePortOut) {
        this.customerVehiclePortOut = customerVehiclePortOut;
    }

    @Override
    @Transactional
    public CustomerVehicle createCustomerVehicle(CustomerVehicle customerVehicle) {
        customerVehiclePolicy.initialize(customerVehicle);
        validateReferences(customerVehicle);
        validateUniqueLicensePlate(customerVehicle);

        customerVehicle.setCustomerVehicleId(UUID.randomUUID());
        CustomerVehicle savedCustomerVehicle = customerVehiclePortOut.save(customerVehicle);
        applySingleDefaultRule(savedCustomerVehicle);
        return savedCustomerVehicle;
    }

    @Override
    @Transactional
    public CustomerVehicle updateCustomerVehicle(UUID customerVehicleId, CustomerVehicle customerVehicle) {
        CustomerVehicle existingCustomerVehicle = getCustomerVehicleById(customerVehicleId);

        existingCustomerVehicle.setVehicleTypeId(customerVehicle.getVehicleTypeId());
        existingCustomerVehicle.setLicensePlate(customerVehicle.getLicensePlate());
        existingCustomerVehicle.setBrand(customerVehicle.getBrand());
        existingCustomerVehicle.setColor(customerVehicle.getColor());
        if (customerVehicle.getIsDefault() != null) {
            existingCustomerVehicle.setIsDefault(customerVehicle.getIsDefault());
        }

        customerVehiclePolicy.validateState(existingCustomerVehicle);
        validateVehicleTypeExists(existingCustomerVehicle.getVehicleTypeId());
        validateUniqueLicensePlate(existingCustomerVehicle, customerVehicleId);

        CustomerVehicle savedCustomerVehicle = customerVehiclePortOut.save(existingCustomerVehicle);
        applySingleDefaultRule(savedCustomerVehicle);
        return savedCustomerVehicle;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerVehicle getCustomerVehicleById(UUID customerVehicleId) {
        return customerVehiclePortOut.findById(customerVehicleId)
                .orElseThrow(() -> new NotFoundException("Customer vehicle not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerVehicle> getCustomerVehicles(
            UUID customerId,
            CustomerVehicleStatus status,
            UUID vehicleTypeId,
            Boolean isDefault,
            String keyword
    ) {
        return customerVehiclePortOut.findAll(customerId, status, vehicleTypeId, isDefault, keyword);
    }

    @Override
    @Transactional
    public void deleteCustomerVehicle(UUID customerVehicleId) {
        CustomerVehicle existingCustomerVehicle = getCustomerVehicleById(customerVehicleId);
        if (existingCustomerVehicle.getStatus() == CustomerVehicleStatus.INACTIVE) {
            return;
        }

        customerVehiclePolicy.inactivate(existingCustomerVehicle);
        customerVehiclePortOut.save(existingCustomerVehicle);
    }

    @Override
    @Transactional
    public CustomerVehicle activateCustomerVehicle(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = getCustomerVehicleById(customerVehicleId);
        customerVehiclePolicy.activate(customerVehicle);
        return customerVehiclePortOut.save(customerVehicle);
    }

    @Override
    @Transactional
    public CustomerVehicle inactivateCustomerVehicle(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = getCustomerVehicleById(customerVehicleId);
        customerVehiclePolicy.inactivate(customerVehicle);
        return customerVehiclePortOut.save(customerVehicle);
    }

    @Override
    @Transactional
    public CustomerVehicle blockCustomerVehicle(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = getCustomerVehicleById(customerVehicleId);
        customerVehiclePolicy.block(customerVehicle);
        return customerVehiclePortOut.save(customerVehicle);
    }

    @Override
    @Transactional
    public CustomerVehicle markCustomerVehicleAsDefault(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = getCustomerVehicleById(customerVehicleId);
        customerVehiclePolicy.markDefault(customerVehicle);
        CustomerVehicle savedCustomerVehicle = customerVehiclePortOut.save(customerVehicle);
        applySingleDefaultRule(savedCustomerVehicle);
        return savedCustomerVehicle;
    }

    @Override
    @Transactional
    public CustomerVehicle unmarkCustomerVehicleAsDefault(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = getCustomerVehicleById(customerVehicleId);
        customerVehiclePolicy.unmarkDefault(customerVehicle);
        return customerVehiclePortOut.save(customerVehicle);
    }

    private void validateReferences(CustomerVehicle customerVehicle) {
        validateCustomerExists(customerVehicle.getCustomerId());
        validateVehicleTypeExists(customerVehicle.getVehicleTypeId());
    }

    private void validateCustomerExists(UUID customerId) {
        if (!customerVehiclePortOut.existsCustomerById(customerId)) {
            throw new NotFoundException("Customer not found");
        }
    }

    private void validateVehicleTypeExists(UUID vehicleTypeId) {
        if (!customerVehiclePortOut.existsVehicleTypeById(vehicleTypeId)) {
            throw new NotFoundException("Vehicle type not found");
        }
    }

    private void validateUniqueLicensePlate(CustomerVehicle customerVehicle) {
        if (customerVehiclePortOut.existsByLicensePlate(customerVehicle.getLicensePlate())) {
            throw new ConflictException("Customer vehicle license plate already exists");
        }
    }

    private void validateUniqueLicensePlate(CustomerVehicle customerVehicle, UUID customerVehicleId) {
        if (customerVehiclePortOut.existsByLicensePlateAndCustomerVehicleIdNot(
                customerVehicle.getLicensePlate(),
                customerVehicleId
        )) {
            throw new ConflictException("Customer vehicle license plate already exists");
        }
    }

    private void applySingleDefaultRule(CustomerVehicle customerVehicle) {
        if (!Boolean.TRUE.equals(customerVehicle.getIsDefault())) {
            return;
        }

        List<CustomerVehicle> defaultVehicles =
                customerVehiclePortOut.findDefaultVehiclesByCustomerId(customerVehicle.getCustomerId());

        for (CustomerVehicle existingDefaultVehicle : defaultVehicles) {
            if (existingDefaultVehicle.getCustomerVehicleId().equals(customerVehicle.getCustomerVehicleId())) {
                continue;
            }
            customerVehiclePolicy.unmarkDefault(existingDefaultVehicle);
            customerVehiclePortOut.save(existingDefaultVehicle);
        }
    }
}
