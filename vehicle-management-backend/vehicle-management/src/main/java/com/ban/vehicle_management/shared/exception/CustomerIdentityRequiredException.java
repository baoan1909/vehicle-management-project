package com.ban.vehicle_management.shared.exception;

/** Business-safe error for an authenticated account without a customer record. */
public class CustomerIdentityRequiredException extends RuntimeException {

    public CustomerIdentityRequiredException(String message) {
        super(message);
    }
}
