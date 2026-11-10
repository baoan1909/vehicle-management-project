package com.ban.vehicle_management.application.parking.checkinspacehold.exception;

import com.ban.vehicle_management.shared.exception.ConflictException;

public class CheckInSpaceHoldExpiredException extends ConflictException {

    public CheckInSpaceHoldExpiredException(String message) {
        super(message);
    }
}
