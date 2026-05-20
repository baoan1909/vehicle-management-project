package com.ban.vehicle_management.domain.people.customervehicle.policy;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class CustomerVehiclePolicy {

    public void initialize(CustomerVehicle customerVehicle) {
        requireCustomerVehicle(customerVehicle);
        requireField(customerVehicle.getCustomerId(), "customerId");
        requireField(customerVehicle.getVehicleTypeId(), "vehicleTypeId");
        customerVehicle.setLicensePlate(TextValidationUtils.normalizeRequiredText(customerVehicle.getLicensePlate(), "licensePlate", 20));
        customerVehicle.setBrand(TextValidationUtils.normalizeNullableText(customerVehicle.getBrand(), "brand", 80));
        customerVehicle.setColor(TextValidationUtils.normalizeNullableText(customerVehicle.getColor(), "color", 50));
        if (customerVehicle.getIsDefault() == null) {
            customerVehicle.setIsDefault(Boolean.FALSE);
        }
        if (customerVehicle.getStatus() == null) {
            customerVehicle.setStatus(CustomerVehicleStatus.ACTIVE);
        }
        validateState(customerVehicle);
    }

    public void activate(CustomerVehicle customerVehicle) {
        requireCustomerVehicle(customerVehicle);
        customerVehicle.setStatus(CustomerVehicleStatus.ACTIVE);
        validateState(customerVehicle);
    }

    public void inactivate(CustomerVehicle customerVehicle) {
        requireCustomerVehicle(customerVehicle);
        customerVehicle.setStatus(CustomerVehicleStatus.INACTIVE);
        customerVehicle.setIsDefault(Boolean.FALSE);
        validateState(customerVehicle);
    }

    public void block(CustomerVehicle customerVehicle) {
        requireCustomerVehicle(customerVehicle);
        customerVehicle.setStatus(CustomerVehicleStatus.BLOCKED);
        customerVehicle.setIsDefault(Boolean.FALSE);
        validateState(customerVehicle);
    }

    public void markDefault(CustomerVehicle customerVehicle) {
        requireCustomerVehicle(customerVehicle);
        if (customerVehicle.getStatus() != CustomerVehicleStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE customer vehicle can be marked as default");
        }
        customerVehicle.setIsDefault(Boolean.TRUE);
        validateState(customerVehicle);
    }

    public void unmarkDefault(CustomerVehicle customerVehicle) {
        requireCustomerVehicle(customerVehicle);
        customerVehicle.setIsDefault(Boolean.FALSE);
        validateState(customerVehicle);
    }

    public void validateState(CustomerVehicle customerVehicle) {
        requireCustomerVehicle(customerVehicle);
        requireField(customerVehicle.getCustomerId(), "customerId");
        requireField(customerVehicle.getVehicleTypeId(), "vehicleTypeId");
        customerVehicle.setLicensePlate(TextValidationUtils.normalizeRequiredText(customerVehicle.getLicensePlate(), "licensePlate", 20));
        customerVehicle.setBrand(TextValidationUtils.normalizeNullableText(customerVehicle.getBrand(), "brand", 80));
        customerVehicle.setColor(TextValidationUtils.normalizeNullableText(customerVehicle.getColor(), "color", 50));
        requireField(customerVehicle.getStatus(), "status");

        if (customerVehicle.getIsDefault() == null) {
            customerVehicle.setIsDefault(Boolean.FALSE);
        }
        if (Boolean.TRUE.equals(customerVehicle.getIsDefault())
                && customerVehicle.getStatus() != CustomerVehicleStatus.ACTIVE) {
            throw new BadRequestException("Default customer vehicle must be ACTIVE");
        }
    }

    private void requireCustomerVehicle(CustomerVehicle customerVehicle) {
        requireField(customerVehicle, "customerVehicle");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

}

