package com.ban.vehicle_management.domain.people.customer.policy;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.shared.enumeration.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.CustomerType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.UUID;

public class CustomerPolicy {

    public void initialize(Customer customer) {
        requireCustomer(customer);
        requireField(customer.getUserProfileId(), "userProfileId");
        customer.setCustomerCode(TextValidationUtils.normalizeCode(customer.getCustomerCode(), "customerCode", 50));
        if (customer.getCustomerType() == null) {
            customer.setCustomerType(CustomerType.REGISTERED);
        }
        if (customer.getApprovalStatus() == null) {
            customer.setApprovalStatus(CustomerApprovalStatus.PENDING);
        }
        validateState(customer);
    }

    public void approve(Customer customer, UUID approvedBy, Instant approvedAt) {
        requireStatus(customer, CustomerApprovalStatus.PENDING);
        requireField(approvedBy, "approvedBy");
        requireField(approvedAt, "approvedAt");

        customer.setApprovedBy(approvedBy);
        customer.setApprovedAt(approvedAt);
        customer.setApprovalStatus(CustomerApprovalStatus.APPROVED);
        validateState(customer);
    }

    public void reject(Customer customer) {
        requirePendingOrApproved(customer);
        customer.setApprovedBy(null);
        customer.setApprovedAt(null);
        customer.setApprovalStatus(CustomerApprovalStatus.REJECTED);
        validateState(customer);
    }

    public void suspend(Customer customer) {
        if (customer == null || customer.getApprovalStatus() != CustomerApprovalStatus.APPROVED) {
            throw new BadRequestException("Customer must be in APPROVED status");
        }
        customer.setApprovedBy(null);
        customer.setApprovedAt(null);
        customer.setApprovalStatus(CustomerApprovalStatus.SUSPENDED);
        validateState(customer);
    }

    public void moveToPending(Customer customer) {
        requireCustomer(customer);
        customer.setApprovedBy(null);
        customer.setApprovedAt(null);
        customer.setApprovalStatus(CustomerApprovalStatus.PENDING);
        validateState(customer);
    }

    public void validateState(Customer customer) {
        requireCustomer(customer);
        requireField(customer.getUserProfileId(), "userProfileId");
        customer.setCustomerCode(TextValidationUtils.normalizeCode(customer.getCustomerCode(), "customerCode", 50));
        requireField(customer.getCustomerType(), "customerType");
        requireField(customer.getApprovalStatus(), "approvalStatus");

        switch (customer.getApprovalStatus()) {
            case APPROVED -> {
                requireField(customer.getApprovedBy(), "approvedBy");
                requireField(customer.getApprovedAt(), "approvedAt");
            }
            case PENDING, REJECTED, SUSPENDED -> {
                if (customer.getApprovedBy() != null || customer.getApprovedAt() != null) {
                    throw new BadRequestException(
                            "approvedBy and approvedAt must be null unless customer is APPROVED");
                }
            }
        }
    }

    private void requirePendingOrApproved(Customer customer) {
        requireCustomer(customer);
        if (customer.getApprovalStatus() != CustomerApprovalStatus.PENDING
                && customer.getApprovalStatus() != CustomerApprovalStatus.APPROVED) {
            throw new BadRequestException("Customer must be in PENDING or APPROVED status");
        }
    }

    private void requireStatus(Customer customer, CustomerApprovalStatus expectedStatus) {
        requireCustomer(customer);
        if (customer.getApprovalStatus() != expectedStatus) {
            throw new BadRequestException("Customer must be in " + expectedStatus + " status");
        }
    }

    private void requireCustomer(Customer customer) {
        requireField(customer, "customer");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

}

