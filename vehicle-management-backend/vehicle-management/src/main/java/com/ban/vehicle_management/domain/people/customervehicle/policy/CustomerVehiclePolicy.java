package com.ban.vehicle_management.domain.people.customervehicle.policy;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;

public class CustomerVehiclePolicy {

    public void initialize(CustomerVehicle customerVehicle) {
        requireCustomerVehicle(customerVehicle);
        requireField(customerVehicle.getCustomerId(), "customerId");
        requireField(customerVehicle.getVehicleTypeId(), "vehicleTypeId");
        customerVehicle.setLicensePlate(normalizeRequired(customerVehicle.getLicensePlate(), "licensePlate"));
        customerVehicle.setBrand(normalizeNullable(customerVehicle.getBrand()));
        customerVehicle.setColor(normalizeNullable(customerVehicle.getColor()));
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
        validateState(customerVehicle);
    }

    public void block(CustomerVehicle customerVehicle) {
        requireCustomerVehicle(customerVehicle);
        customerVehicle.setStatus(CustomerVehicleStatus.BLOCKED);
        validateState(customerVehicle);
    }

    public void markDefault(CustomerVehicle customerVehicle) {
        requireCustomerVehicle(customerVehicle);
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
        customerVehicle.setLicensePlate(normalizeRequired(customerVehicle.getLicensePlate(), "licensePlate"));
        customerVehicle.setBrand(normalizeNullable(customerVehicle.getBrand()));
        customerVehicle.setColor(normalizeNullable(customerVehicle.getColor()));
        requireField(customerVehicle.getStatus(), "status");

        if (customerVehicle.getIsDefault() == null) {
            customerVehicle.setIsDefault(Boolean.FALSE);
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

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = normalizeNullable(value);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}

